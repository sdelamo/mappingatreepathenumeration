package pathenumeration

import demo.Comment
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Specification

@Rollback
@Integration
class CommentServiceIntegrationSpec extends Specification {

    @Autowired
    CommentService commentService

    List<Map> expected = [
            [a: 1, path: '1/', l: 1],
            [a: 2, path: '1/2/', l: 2],
            [a: 3, path: '1/2/3/', l: 3],
            [a: 4, path: '1/4/', l: 2],
            [a: 5, path: '1/4/5/', l: 3],
            [a: 6, path: '1/4/6/', l: 3],
            [a: 7, path: '1/4/6/7/', l: 4],
    ]

    Comment c1
    Comment c2
    Comment c3
    Comment c4
    Comment c5
    Comment c6
    Comment c7

    def setup() {
        CommentGormEntity.withTransaction {
            CommentGormEntity.deleteAll()
        }
        CommentGormEntity.withTransaction {
            c1 = commentService.saveComment('What\'s the cause of this bug?', 'Fran', null)
        }
        CommentGormEntity.withTransaction {
            c2 = commentService.saveComment('I think it is a null pointer', 'Ollie', c1)
        }
        CommentGormEntity.withTransaction {
            c3 = commentService.saveComment('No, I checked for that', 'Fran', c2)
        }
        CommentGormEntity.withTransaction {
            c4 = commentService.saveComment('We ned to check for valid input', 'Kukla', c1)
        }
        CommentGormEntity.withTransaction {
            c5 = commentService.saveComment('Yes, that\'s a bug', 'Ollie', c4)
        }
        CommentGormEntity.withTransaction {
            c6 = commentService.saveComment('Yes, please add a check', 'Fran', c4)
        }
        CommentGormEntity.withTransaction {
            c7 = commentService.saveComment('That fixed it', 'Kukla', c6)
        }
    }

    def cleanup() {
        CommentGormEntity.withTransaction {
            commentService.deleteComment(c1)
        }
    }

    def 'test comment and comment tree path is generated correctly'() {
        when:
        def comments = CommentGormEntity.findAll()
        def ids = comments.collect { [a: it.id, path: it.path, l: it.length] }


        then:
        expected.size() == ids.size()
        for (Map m : ids) {
            assert expected.find { it.a == m.a && it.d == m.d && it.l == m.l }
        }
    }

    @Ignore
    def 'insert a new node below #5'() {
        when:
        commentService.saveComment('I agree', 'Fran', c5)

        then:
        CommentGormEntity.count() == (expected.size() + 1)
    }

    @Ignore
    def 'move node'() {
        when:
        commentService.moveCommentToParent(c6, c3)

        then:
        CommentGormEntity.count() == expected.size()
    }

    def 'query inmediate child'() {
        when:
        def result = commentService.childsOfComment(c4)

        then:
        CommentGormEntity.count() == expected.size()
        result.size() == 2
        result.find { it.id = c5.id }
        result.find { it.id = c6.id }
    }

    def 'query inmediate parent'() {
        when:
        def result = commentService.parentsOfComment(c1)

        then:
        !result

        when:
        CommentGormEntity.count() == expected.size()
        result = commentService.parentsOfComment(c4)

        then:
        result
        result.size() == 1
        result.find { it.id = c1.id }
    }

    def 'query descendants of #4'() {
        when:
        def descendants = commentService.descendantsOfComment(c4)

        then:
        CommentGormEntity.count() == expected.size()
        descendants
        descendants.size() == 3
        descendants.find { it.id == c6.id}
        descendants.find { it.id == c5.id}
        descendants.find { it.id == c7.id}
    }

    def 'query ancestors of #7'() {
        when:
        def ancestors = commentService.ancestorsOfComment(c1)

        then:
        !ancestors

        when:
        ancestors = commentService.ancestorsOfComment(c4)

        then:
        ancestors
        ancestors.size() == 1
        ancestors.first().id == c1.id

        when:
        ancestors = commentService.ancestorsOfComment(c2)

        then:
        ancestors
        ancestors.size() == 1
        ancestors.first().id == c1.id

        when:
        ancestors = commentService.ancestorsOfComment(c7)

        then:
        CommentGormEntity.count() == expected.size()
        ancestors
        ancestors.find { it.id == c6.id}
        ancestors.find { it.id == c4.id}
        ancestors.find { it.id == c1.id}
        ancestors.size() == 3
    }
}
