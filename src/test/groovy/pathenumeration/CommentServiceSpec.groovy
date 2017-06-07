package pathenumeration

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(CommentService)
class CommentServiceSpec extends Specification {

    @Unroll
    def "pathsForPath returns #paths for path"(String path, List<String> paths) {
        when:
        def result = service.pathsForPath(path)

        then:
        result == paths

        where:
        path      || paths
        '1/4/6/7' || ['1/4/6/%','1/4/%','1/%']
    }
}
