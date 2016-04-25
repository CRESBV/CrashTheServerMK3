import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Edward on 4/25/2016.
 */
public class ReadFiles {
    public static List<List<String>> getEventNames(String fileName){
        Path filePath = Paths.get(fileName);
        List<List<String>> output = new ArrayList<>();
        List<String> teams = new ArrayList<>();
        List<String> events = new ArrayList<>();
        List<String> temp = new ArrayList<>();
        try {
            temp = Files.readAllLines(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean startTeams = false;
        for (String currentElement:temp) {
            if (currentElement.equalsIgnoreCase("#teams")) {
                startTeams = true;
                continue;
            }
            if(currentElement.equalsIgnoreCase("#events")){
                continue;
            }
            if(startTeams){
                teams.add(currentElement);
            }
            else {
                events.add(currentElement);
            }
        }
        output.add(events);
        output.add(teams);
        return output;
    }

    public static void main(String[] args) {
        System.out.println(getEventNames("C:\\Users\\Edward\\Desktop\\Champs2016\\Test1.txt"));
    }
}
