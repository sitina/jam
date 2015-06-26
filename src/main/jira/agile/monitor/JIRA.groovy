package jira.agile.monitor


import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import static org.fusesource.jansi.Ansi.*
import groovyx.net.http.*

import java.text.SimpleDateFormat

import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.protocol.HttpContext
import org.fusesource.jansi.AnsiConsole


/**
 * java -jar jam-0.0.1-SNAPSHOT-jar-with-dependencies.jar -t DDT-525,DDT-493,DDT-495,DDT-697,DDT-686,DDT-683 -u dezider.mesko
 * mvn clean compile assembly:single
 * @author dezider.mesko
 *
 */
class JAM {

    def static JIRA_REST_URL = "https://jira.intgdc.com/rest"
    def static JIRA_API_URL = JIRA_REST_URL + "/api/latest/"
    def static JIRA_AGILE_URL = JIRA_REST_URL + "/greenhopper/1.0/"

    public static void main(String[] args) {

        def jam = new JAM()

        System.setProperty("jansi.passthrough", "true");
        AnsiConsole.systemInstall();

        def String authString = getAuthString(args)
        def jira = new RESTClient(JIRA_API_URL);
        jira.handler.failure = { resp -> println "Unexpected failure: ${resp.statusLine}"; System.exit(1) }
        setupAuthorization(jira, authString)

        def jira_agile = new RESTClient(JIRA_AGILE_URL);
        jira_agile.handler.failure = { resp -> println "Unexpected failure: ${resp.statusLine}" }
        setupAuthorization(jira_agile, authString)

        List<String> loaded_stories = []

        try {
            // https://jira.intgdc.com/rest/greenhopper/1.0/xboard/work/allData.json?rapidViewId=149&_=1434382361552
            def stories_sprint = jira_agile.get(path: "xboard/work/allData.json", requestContentType: JSON, queryString: "rapidViewId=149")
            def rawSprintReport = stories_sprint.getData()

            rawSprintReport.issuesData.issues.each {
                if (it.typeName != "Sub-task") {
                    loaded_stories.add(it.key)
                }
            }
        } catch (Exception e) {
            println(e)
        }


        def sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm")
        println "Status at " + sdf.format(new Date())

        loaded_stories.each {
            def story = jira.get(path: "issue/${it}", requestContentType: JSON).getData()
            def rawSubTasks = jira.get(path: 'search', query: ["jql":"parent=${it}", "expand":"changelog"]).getData()
            jam.processSubtasks(rawSubTasks, story)
        }

        AnsiConsole.systemUninstall();
    }

    def processSubtasks(rawSubTasks, story) {
        def subTasksList = []
        def stry = new Story(raw:story, id:story.key, status:story.fields.status.name, description:story.fields.summary, user:story.fields.assignee.name)

        rawSubTasks.issues.each{ it ->
            def st = new SubTask(raw:it, description:it.fields.summary, user:it.fields.assignee.name, status:it.fields.status.name, id:it.key)
            subTasksList.add(st)
        }

        if(subTasksList.isEmpty()) {
            println("\n" + stry.toString())
        } else {
            println "\n"+story.key+": "+story.fields.summary
            Collections.sort(subTasksList)
            def total = 0
            def done = 0
            subTasksList.each {
                println it
            }
        }
    }

    def static setupAuthorization(RESTClient jira, String authString) {
        jira.ignoreSSLIssues()

        jira.client.addRequestInterceptor(
                new HttpRequestInterceptor() {
                    void process(HttpRequest httpRequest,
                            HttpContext httpContext) {
                        httpRequest.addHeader('Authorization', "Basic "+authString)
                    }
                })
    }

    private static String getAuthString(String[] args) {
        def user = null
        def pw = null

        user = getArgument(args, "-u", "Username parameter missing")
        pw = getArgument(args, "-p", "Password parameter missing")

        if(user == null){
            user = System.console().readLine("Your JIRA username: ")
        } else {
            println "Using username: "+user
        }
        if(pw == null){
            pw = System.console().readPassword("Your JIRA password: ");
        }

        def authString = "${user}:${pw}".getBytes().encodeBase64().toString()
        return authString
    }

    private static getArgument(args, String option, String errorMessage){
        def uIndex = args.findIndexOf { it.equals(option) } + 1
        if (uIndex != 0){
            if (uIndex >= args.length){
                println errorMessage
                return null
            }
            return args[uIndex];
        }
        return null
    }
}
