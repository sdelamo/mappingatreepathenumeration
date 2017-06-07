package pathenumeration

import demo.Comment
import demo.TreeService
import grails.compiler.GrailsCompileStatic
import grails.gorm.DetachedCriteria
import grails.transaction.Transactional
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j

@Slf4j
@GrailsCompileStatic
class CommentService  implements TreeService {

    public static final String SEPARATOR = '/'

    @Transactional(readOnly = true)
    @Override
    Comment read(Long id) {
        CommentGormEntity.read(id)
    }

    @Transactional(readOnly = true)
    @Override
    List<Comment> descendantsOfComment(Comment comment) {
        descendantsOfCommentCriteria(comment).list()
    }

    @CompileDynamic
    @Transactional(readOnly = true)
    @Override
    List<Comment> childsOfComment(Comment comment) {
        CommentGormEntity commentEntity = comment as CommentGormEntity
        Integer childrenLength = (commentEntity.length + 1)
        def pathQuery = "${commentEntity?.path}%" as String
        CommentGormEntity.createCriteria().list {
                eq('length', childrenLength)
                like('path', pathQuery)
                ne('id', commentEntity.id)

        }
    }

    private DetachedCriteria<CommentGormEntity> descendantsOfCommentCriteria(Comment comment,  boolean excludeSelf = true) {
        CommentGormEntity commentEntity = (comment as CommentGormEntity)
        def pathQuery = "${commentEntity?.path}%" as String
        println commentEntity
        def q = CommentGormEntity.where {
            path ==~ pathQuery
        }
        if ( excludeSelf ) {
            return q.where {
                id != commentEntity.id
            }
        }
        q
    }

    @Transactional(readOnly = true)
    @Override
    @CompileDynamic
    List<Comment> ancestorsOfComment(Comment comment) {
        CommentGormEntity commentEntity = (comment as CommentGormEntity)
        final String path = commentEntity.path
        List<String> paths = pathsForPath(path)
        if ( !paths ) {
            return [] as List<Comment>
        }

        CommentGormEntity.createCriteria().list {
            or {
                paths.each { String subpath ->
                    and {
                        like('path', subpath)
                        eq('length', (subpath.split(SEPARATOR).size() - 1 ))
                    }

                }
            }
            ne('id', comment.id)
        }
    }

    List<String> pathsForPath(final String path) {
        path.tokenize(SEPARATOR).subsequences().collect {
            "${it.join(SEPARATOR)}${SEPARATOR}"
        }.findAll {
            path.startsWith(it) && path != it
        }.collect {
            "${it}%" as String
        }.sort { a, b ->
            b.split(SEPARATOR).size() <=> a.split(SEPARATOR).size()
        }
    }

    @CompileDynamic
    @Transactional(readOnly = true)
    @Override
    List<Comment> parentsOfComment(Comment commentEntity) {
        ancestorsOfComment(commentEntity).findAll {
            (it as CommentGormEntity).length == ((commentEntity as CommentGormEntity).length - 1)
        }
    }

    @Override
    @Transactional
    void deleteComment(Comment comment) {
        def comments = [comment]
        comments += descendantsOfComment(comment)
        comments.sort { a, b -> (b as CommentGormEntity).length <=> (a as CommentGormEntity).length }
        comments.each {
            (it as CommentGormEntity).delete(failOnError: true)
        }
    }

    @Transactional
    @Override
    void moveCommentToParent(Comment comment, Comment newAncestor) {

    }

    @Transactional
    Comment saveComment(String comment, String author, Comment ancestorComment) {
        CommentGormEntity c
        CommentGormEntity.withTransaction {
            c = new CommentGormEntity(comment: comment, author: author)
            c.path = (ancestorComment as CommentGormEntity)?.path ?: ''
            c.length = c.path.split(SEPARATOR).size()
            if ( !c.save() ) {
                log.error "Could not save comment ${c.errors}"
            }
        }
        if ( c?.hasErrors() ) {
            return null
        }
        CommentGormEntity.withTransaction {
            c.path += "${c.id}$SEPARATOR"
            c.length = c.path.split(SEPARATOR).size()
            if ( !c.save() ) {
                log.error "Could not set path for comment ${c.errors}"
            }
        }
        c
    }
}
