/**
 * Created by Edward on 4/13/2016.
 */
import com.plnyyanks.tba.apiv2.APIv2Helper;
import com.plnyyanks.tba.apiv2.interfaces.APIv2;
import com.plnyyanks.tba.apiv2.models.District;
import com.plnyyanks.tba.apiv2.models.Event;
import com.plnyyanks.tba.apiv2.models.Match;
import com.plnyyanks.tba.apiv2.models.Team;

import java.util.List;

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
        Match matches=api.fetchMatch("2016nyro_f1m1",null);
        System.out.println(matches.getKey()+matches.getAlliances().toString());
        System.out.println(matches.getScore_breakdown().toString());

        for (Event event : events) {
            List<Match> matchList=api.fetchEventMatches(event.getKey(),null);
            for (Match match : matchList) {
                //System.out.println(match.getAlliances().toString());
                String teamsInString=match.getAlliances().getAsJsonObject("blue").toString()+
                        match.getAlliances().getAsJsonObject("red").toString();
                boolean is639=teamsInString.contains("frc639");
                if(is639) System.out.println(match.getKey()+match.getAlliances().toString());
                if(is639) System.out.println(matches.getScore_breakdown().toString());
            }
        }


/*        districts.stream()
                .forEach(distName -> System.out.println(distName.getKey()+"\t"+distName.getName()));*/

        Team uberbots = api.fetchTeam("frc639", null); // Add 'If-Modified-Since' header (String) as the second parameter
        System.out.println("Got team with key: "+uberbots.getKey()+"\n"+uberbots.getNickname()+uberbots.getLocality());
    }
}