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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by edbuckler on 4/9/16.
 */
public class GetAllTheDataV2 {

    //private static final String API_APP_ID = "plnyyanks:apiv2-java-junit:v0.1";
    private static final String API_APP_ID = "frc639:scouting-system:v01";
    //https://docs.google.com/spreadsheets/d/1HqsReMjr5uBuyZjqv14t6bQF2n038GfMmWi3B6vFGiA/edit#gid=24

    public static void main(String[] args) {
        APIv2Helper.setAppId(API_APP_ID);
        APIv2 api = APIv2Helper.getAPI();
        System.out.println("Hello World");
//        List<District> districts = api.fetchDistrictList(2016, null);
        // List<Event> events = api.fetchDistrictEvents("in", 2016, null);
        List<Event> events = api.fetchEventsInYear(2016,null);
        events.stream().forEach(ev -> System.out.println(ev.getKey()));
//        Match matches=api.fetchMatch("2016nyro_f1m1",null);
//        System.out.println(matches.getKey()+matches.getAlliances().toString());
//        System.out.println(matches.getScore_breakdown().toString());

        Table<String,String,List<Double>> dataTable= TreeBasedTable.create();
        for (Event event : events) {
            List<Match> matchList=api.fetchEventMatches(event.getKey(),null);
            matchList.stream()//.limit(100)
                    .forEach(match -> {
                        List<String> allianceColors = Lists.newArrayList("blue", "red");
                        for (String allianceColor : allianceColors) {


//                        String teamsInString=match.getAlliances().getAsJsonObject("blue").toString()+
//                                match.getAlliances().getAsJsonObject("red").toString();
                            List<String> oneAlliance = new ArrayList<>();
                            for (JsonElement blueTeam : match.getAlliances().getAsJsonObject(allianceColor).getAsJsonArray("teams")) {
                                oneAlliance.add(blueTeam.getAsString());
                            }

                            JsonObject scoreBreak = null;
                            try {
                                scoreBreak = match.getScore_breakdown().getAsJsonObject().getAsJsonObject(allianceColor);
                            } catch (IllegalStateException ise) {
                                System.err.println(match.toString());
                            }
                            if (scoreBreak == null) return;
                            addScoresToDataTable(dataTable, scoreBreak, oneAlliance);
                        }
                    });
        }
        System.out.println(makeNiceLookingTable(dataTable));
        Path output = Paths.get("/Users/edbuckler/Downloads/CrashTheServerOutput.txt");
        try {
            Files.write(output, makeNiceLookingTable(dataTable).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(dataTable.toString());

    }

    private static void addScoresToDataTable(Table<String,String,List<Double>> dataTable, JsonObject scoreBreak,
                                             List<String> oneAlliance) {
        Map<String,String> positionMap=createPositionMap(scoreBreak);
        positionMap.put("position1crossings","Low_Bar");
        for (String aTeam : oneAlliance) {
            scoreBreak.entrySet().stream()
                    .forEach(es -> {
                        if(es.getKey().equals("towerFaceC")) {
                            // System.out.println(blueTeam+":"+es.toString());
                        }
                        if(aTeam.equals("frc3354")) {
                            System.out.println(aTeam+":"+es.toString());
                        }
                        String finalKey=es.getKey();
                        //all key keep their original names except for the position crossings
                        if(positionMap.containsKey(es.getKey())) {
                            finalKey=positionMap.get(es.getKey());
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
                //.peek(es -> System.out.println(es.toString()))
                .collect(Collectors.toMap(es -> es.getKey()+"crossings", es -> es.getValue().getAsString()));

    }

    private static String makeNiceLookingTable(Table<String,String,List<Double>> dataTable) {
        String delimiter="\t";
        final StringBuilder sb=new StringBuilder();
        List<String> attributes=new ArrayList<>(dataTable.columnKeySet());
        sb.append(dataTable.columnKeySet().stream().collect(Collectors.joining(delimiter,"Team"+delimiter,"")));
        sb.append("\n");
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
            int matchCount = dataTable.get(teamNames, "totalPoints").size();
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