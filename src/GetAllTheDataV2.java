/**
 * Created by Edward on 4/13/2016.
 */
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.primitives.Doubles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.plnyyanks.tba.apiv2.APIv2Helper;
import com.plnyyanks.tba.apiv2.interfaces.APIv2;
import com.plnyyanks.tba.apiv2.models.Event;
import com.plnyyanks.tba.apiv2.models.Match;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by edbuckler on 4/9/16.
 */
public class GetAllTheDataV2 {

    //private static final String API_APP_ID = "plnyyanks:apiv2-java-junit:v0.1";
    private static final String API_APP_ID = "frc639:scouting-system:v01";
    //https://docs.google.com/spreadsheets/d/1HqsReMjr5uBuyZjqv14t6bQF2n038GfMmWi3B6vFGiA/edit#gid=24

    public static void main(String[] args) {
        //args restrict by event and/or by team
        APIv2Helper.setAppId(API_APP_ID);
        APIv2 api = APIv2Helper.getAPI();
        System.out.println("Hello World");
//        List<District> districts = api.fetchDistrictList(2016, null);
        // List<Event> events = api.fetchDistrictEvents("in", 2016, null);
        List<Event> events = api.fetchEventsInYear(2016,null);
        events.stream().forEach(ev -> System.out.println(ev.getKey()+"\t"+ev.getStart_date()));

//        Match matches=api.fetchMatch("2016nyro_f1m1",null);
//        System.out.println(matches.getKey()+matches.getAlliances().toString());
//        System.out.println(matches.getScore_breakdown().toString());

        Table<String,String,List<Double>> dataTable= TreeBasedTable.create();
        Table<String,String,Integer> powerTable= TreeBasedTable.create();
        Map<String, Double> attributeSum = new TreeMap<>();

        events.stream()
                .limit(5)
                .forEach(event -> {
                    List<Match> matchList = api.fetchEventMatches(event.getKey(), null);
                    matchList.stream()//.limit(10)
                            .forEach(match -> {
                                List<String> allianceColors = Lists.newArrayList("blue", "red");
                                for (String allianceColor : allianceColors) {
                                    List<String> oneAlliance = new ArrayList<>();
                                    for (JsonElement blueTeam : match.getAlliances().getAsJsonObject(allianceColor).getAsJsonArray("teams")) {
                                        oneAlliance.add(blueTeam.getAsString());
                                    }

                                    JsonObject scoreBreak = null;
                                    JsonObject scoreBreakDef = null;
                                    try {
                                        scoreBreak = match.getScore_breakdown().getAsJsonObject().getAsJsonObject(allianceColor);
                                        //opposite
                                        scoreBreakDef = match.getScore_breakdown().getAsJsonObject().getAsJsonObject(allianceColor.equals("blue") ? "red" : "blue");
                                    } catch (IllegalStateException ise) {
                                        System.err.println("Match scores failing for:" + match.getKey().toString());
                                    }
                                    if (scoreBreak == null || scoreBreakDef == null) return;
                                    addScoresToDataTable(dataTable, scoreBreak, oneAlliance, true);
                                    addScoresToDataTable(dataTable, scoreBreakDef, oneAlliance, false);
                                    addScoresToPowerTable(powerTable, attributeSum, scoreBreak, oneAlliance, "totalPoints", true);
                                }
                            });
                });

        System.out.println(makeNiceLookingTable(dataTable));

        Path output = Paths.get("/Users/edbuckler/Downloads/CrashTheServerOutput.txt");
        try {
            Files.write(output, makeNiceLookingTable(dataTable).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(dataTable.toString());

    }

    private static void estimatePowerScores(Table<String,String,Integer> powerTable, Map<String, Double> attributeSum) {

    }

    private static void addScoresToPowerTable(Table<String,String,Integer> powerTable, Map<String, Double> attributeSum,
                                              JsonObject scoreBreak, List<String> oneAlliance, String attribute, boolean offenseOrDefence) {
        final String prefix=(offenseOrDefence)?"Off_":"Def_";
        Map<String,String> positionMap=createPositionMap(scoreBreak);
        positionMap.put("position1crossings","Low_Bar");

        scoreBreak.entrySet().stream()
                .filter(es -> es.getKey().equals(attribute))
                .forEach(es -> {
                    String finalKey=prefix+es.getKey();
                    Double finalValue=Double.NaN;
                    String valueAsString=es.getValue().getAsString();
                    if(Doubles.tryParse(valueAsString)!=null) {
                        finalValue=Double.parseDouble(valueAsString);
                    } else if(valueAsString.equals("true")) {
                        finalValue=1.0;
                    } else if(valueAsString.equals("false")) {
                        finalValue=0.0;
                    } else if(valueAsString.equals("Challenged") || valueAsString.equals("Scaled") || valueAsString.equals("Both")) {
                        finalValue=1.0;
                    }  else if(valueAsString.equals("Unknown")) {
                        finalValue=0.0;
                    }  else if(valueAsString.equals("None")) {
                        finalValue=0.0;
                    } else {
                        finalValue=Double.NaN;
                    }
                    if(Double.isNaN(finalValue)) return;
                    final double fv=finalValue;
                    for (String aTeam : oneAlliance) {
                        for (String bTeam : oneAlliance) {
                            Integer value=powerTable.get(aTeam,bTeam);
                            if(value==null) {value=1;}
                            else {value=value+1;}
                            powerTable.put(aTeam, bTeam, value);
                        }
                        attributeSum.merge(aTeam, fv, (ov, nv) -> ov + nv);
                    }
                });

    }


    private static void addScoresToDataTable(Table<String,String,List<Double>> dataTable, JsonObject scoreBreak,
                                             List<String> oneAlliance, boolean offenseOrDefence) {
        final String prefix=(offenseOrDefence)?"Off_":"Def_";
        Map<String,String> positionMap=createPositionMap(scoreBreak);
        positionMap.put("position1crossings","Low_Bar");
        for (String aTeam : oneAlliance) {
            scoreBreak.entrySet().stream()
                    .filter(es -> !es.getKey().matches("position\\d"))
                    .filter(es -> !es.getKey().startsWith("robot"))
                    .filter(es -> !es.getKey().startsWith("adjustPoints"))
                    .forEach(es -> {
                        String finalKey=prefix+es.getKey();
                        //all key keep their original names except for the position crossings
                        if(positionMap.containsKey(es.getKey())) {
                            finalKey=prefix+positionMap.get(es.getKey());
                        } else {
                            //This deals with a rare situation then defense is not specified for a crossing
                            if(es.getKey().matches("position\\dcrossings")) return;
                        }

                        Double finalValue=Double.NaN;
                        String valueAsString=es.getValue().getAsString();
                        if(Doubles.tryParse(valueAsString)!=null) {
                            finalValue=Double.parseDouble(valueAsString);
                        } else if(valueAsString.equals("true")) {
                            finalValue=1.0;
                        } else if(valueAsString.equals("false")) {
                            finalValue=0.0;
                        } else if(valueAsString.equals("Challenged") || valueAsString.equals("Scaled") || valueAsString.equals("Both")) {
                            finalValue=1.0;
                        }  else if(valueAsString.equals("Unknown")) {
                            finalValue=0.0;
                        }  else if(valueAsString.equals("None")) {
                            finalValue=0.0;
                        } else {
                            finalValue=Double.NaN;
                        }
                        if(es.getKey().equals("towerFaceC") && (Double.isNaN(finalValue))) {
                            System.err.println(aTeam+":"+es.toString());
                        }
                        List<Double> values = dataTable.get(aTeam, finalKey);
                        if (values == null) {
                            values = new ArrayList<>();
                        }
                        values.add(finalValue);
                        dataTable.put(aTeam, finalKey, values);
                    });
        }

    }

    private static Map<String,String> createPositionMap(JsonObject scoreBreak) {
        String regex = "position\\d";
        return scoreBreak.entrySet().stream()
                .filter(es -> es.getKey().matches(regex))
                .filter(es -> !es.getValue().getAsString().startsWith("NotSpecified"))
                //.peek(es -> System.out.println(es.toString()))
                .collect(Collectors.toMap(es -> es.getKey()+"crossings", es -> es.getValue().getAsString()));

    }

    private static String makeNiceLookingTable(Table<String,String,List<Double>> dataTable) {
        String delimiter="\t";
        final StringBuilder sb=new StringBuilder();
        List<String> attributes=new ArrayList<>(dataTable.columnKeySet());
        sb.append(dataTable.columnKeySet().stream().collect(Collectors.joining(delimiter,"Team"+delimiter,"")));
        sb.append(delimiter+"MatchCount\n");
        dataTable.rowKeySet().stream().forEach(teamNames -> {
            sb.append(teamNames+delimiter);
            attributes.stream().forEach(att -> {
                List<Double> valArray=dataTable.get(teamNames,att);
                if(valArray==null) {
                    sb.append(Double.NEGATIVE_INFINITY+delimiter);
                } else {
                    //sb.append(valArray.toString()+"=");
                    sb.append(valArray.stream().mapToDouble(d -> d)
                            .filter(d -> !Double.isNaN(d))
                            .average().orElse(Double.NEGATIVE_INFINITY) + delimiter);

                }
            });
            int matchCount = dataTable.get(teamNames, "Off_totalPoints").size();
            sb.append(matchCount);
//            dataTable.row(teamNames).values().stream()
//                    .forEach(valArray -> {
//                        sb.append(valArray.stream().mapToDouble(d -> d).average().orElse(Double.NEGATIVE_INFINITY)+delimiter);
//                        //sb.append(valArray.toString()+delimiter);
//                    });
            sb.append("\n");

        });
        return sb.toString();
    }


}