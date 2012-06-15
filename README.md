This service adds sweetp hooks to git flow commands. When a git flow
command is issued, this service will execute hooks so other services
can act on these. If a force switch is supplied, the original git flow
command is executed in every case.

Current commands with hooks are:

*   feature finish
    * /scm/preMerge
    * /scm/postMerge

For example the [scm enhancer service](https://github.com/sweetp/service-scmEnhancer)
provides a "/scm/preMerge" hook
to check if there are any fixup commits in your feature branch. If this
is the case you should "git rebase -i develop" to fix them up before
you merge your changes with the develop branch.

For basic information see
[boilerplate service](https://github.com/sweetp/service-boilerplate-groovy).

More Information on [sweet productivity](http://sweet-productivity.com).
