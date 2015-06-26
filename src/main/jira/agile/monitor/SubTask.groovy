package jira.agile.monitor

import static org.fusesource.jansi.Ansi.*
import static org.fusesource.jansi.Ansi.Color.*

import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.Hours
import org.joda.time.format.DateTimeFormat

class SubTask extends Task implements Comparable{

    def int compareTo(Object o) {
        def st = ((SubTask) o).status

        if(this.status.equals(st)) return 0
        if(this.status.equals(DONE)) return 1
        if(this.status.equals(TO_DO)) return -1
        if(this.status.equals(IN_PROGRESS)){
            if(st.equals(DONE)) return -1
            if(st.equals(TO_DO)) return 1
        }
        return 0
    }

    def String toString(){
        return "${getColorStatus()} ${getAbbreivation()} ${id}: ${description}"
    }
}
