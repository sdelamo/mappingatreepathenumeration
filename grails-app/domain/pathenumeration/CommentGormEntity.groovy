package pathenumeration

import grails.compiler.GrailsCompileStatic
import demo.Comment
 
@GrailsCompileStatic
class CommentGormEntity implements Serializable, Comment {
    String comment
    String author
    String path
    Integer length

    static constraints = {
        comment nullable: false
        path nullable: false, blank: true
        author nullable: false
        length nullable: false, min: 0
    }

    static mapping = {
        version false
        table 'comment'
        comment type: 'text'
        path sqlType: 'varchar', length: 1000
    }

    String toString() {
        "(${id} ${author}: ${comment} $length $path"
    }
}
