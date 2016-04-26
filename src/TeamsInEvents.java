import com.plnyyanks.tba.apiv2.APIv2Helper;
import com.plnyyanks.tba.apiv2.interfaces.APIv2;
import com.plnyyanks.tba.apiv2.models.Event;
import com.plnyyanks.tba.apiv2.models.Team;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by edbuckler on 4/25/16.
 */
public class TeamsInEvents {

    private static final String API_APP_ID = "frc639:scouting-system:v01";

    public static void main(String[] args) {

        APIv2Helper.setAppId(API_APP_ID);
        APIv2 api = APIv2Helper.getAPI();
        List<Event> events = api.fetchEventsInYear(2016,null);
        events.stream()
                .filter(ev -> ev.getStart_date().startsWith("2016-04-2"))
                .forEach(ev -> {
                    System.out.println(ev.getKey()+"\t"+ev.getStart_date());
                    List<Team> teams=api.fetchEventTeams(ev.getKey(),null);
                    if(teams!=null) {
                        teams.stream().forEach(team -> System.out.println(team.getKey()));
                    }
                    System.out.println();
                });

        System.out.println("Hello World");
    }
}
