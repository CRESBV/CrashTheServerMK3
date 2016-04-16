/**
 * Created by Edward on 4/13/2016.
 */
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.primitives.Doubles;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.plnyyanks.tba.apiv2.APIv2Helper;
import com.plnyyanks.tba.apiv2.interfaces.APIv2;
import com.plnyyanks.tba.apiv2.models.District;
import com.plnyyanks.tba.apiv2.models.Event;
import com.plnyyanks.tba.apiv2.models.Match;
import com.plnyyanks.tba.apiv2.models.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by edbuckler on 4/9/16.
 */
public class GetAllTheData {

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
            matchList.stream().limit(100)
                    .forEach(match -> {
                        String teamsInString=match.getAlliances().getAsJsonObject("blue").toString()+
                                match.getAlliances().getAsJsonObject("red").toString();
                        List<String> blueTeams= new ArrayList<>();
                        for (JsonElement blueTeam : match.getAlliances().getAsJsonObject("blue").getAsJsonArray("teams")) {
                            blueTeams.add(blueTeam.getAsString());
                        }
                        Double score=match.getAlliances().getAsJsonObject("blue").get("score").getAsDouble();
                        System.out.println(blueTeams.toString());

                        for (String blueTeam : blueTeams) {
                            List<Double> values=dataTable.get(blueTeam,"score");
                            if(values==null) {
                                values=new ArrayList<>();
                            }
                            values.add(score);
                            dataTable.put(blueTeam,"score",values);
                        }
                        JsonObject scoreBreak=null;
                        try{
                            scoreBreak=match.getScore_breakdown().getAsJsonObject().getAsJsonObject("blue");
                        } catch (IllegalStateException ise) {
                            System.err.println(match.toString());
                        }
                        if(scoreBreak==null) return;
                        for (String blueTeam : blueTeams) {
                            scoreBreak.entrySet().stream()
                                    .forEach(es -> {
                                        System.out.println(es.toString());
                                        Double value=Double.NaN;
                                        String valueAsString=es.getValue().getAsString();
                                        if(Doubles.tryParse(valueAsString)!=null) {
                                            value=Double.parseDouble(valueAsString);
                                        } else if(valueAsString.equals("true")) {
                                            value=1.0;
                                        } else if(valueAsString.equals("false")) {
                                            value=0.0;
                                        } else if(valueAsString.equals("Challenged")) {
                                            value=1.0;
                                        }  else if(valueAsString.equals("None")) {
                                            value=0.0;
                                        } else {
                                            value=Double.NaN;
                                        }
                                        List<Double> values = dataTable.get(blueTeam, es.getKey());
                                        if (values == null) {
                                            values = new ArrayList<>();
                                        }
                                        values.add(value);
                                        dataTable.put(blueTeam, es.getKey(), values);
                                    });
                        }
//                        System.out.println(scoreBreak);

//
//
//                        boolean is639=teamsInString.contains("frc639");
//                        if(is639) System.out.println(match.getKey()+match.getAlliances().toString());
//                        if(is639) System.out.println(matches.getScore_breakdown().toString());
                    });
            break;
        }
        System.out.println(dataTable.toString());


/*        districts.stream()
                .forEach(distName -> System.out.println(distName.getKey()+"\t"+distName.getName()));*/
//
//        Team uberbots = api.fetchTeam("frc639", null); // Add 'If-Modified-Since' header (String) as the second parameter
//        System.out.println("Got team with key: "+uberbots.getKey()+"\n"+uberbots.getNickname()+uberbots.getLocality());
    }


}