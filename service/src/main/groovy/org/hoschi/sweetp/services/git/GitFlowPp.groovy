package org.hoschi.sweetp.services.git

import groovy.json.JsonBuilder
import groovy.util.logging.Log4j
import org.hoschi.sweetp.services.base.IHookManager
import org.hoschi.sweetp.services.base.IRouter
import org.hoschi.sweetp.services.base.ServiceParameter

/**
 * Service for 'git flow' but supports service hooks.
 *
 * @author Stefan Gojan
 */
@Log4j
class GitFlowPp {
	String config
	IRouter router

	/**
	 * Config example for this service.
	 */
	static final String CONFIG_EXAMPLE = '''
<pre>
{
    "name": "testgit",
        "git": {
                "dir":".git"
        },
        "scm": {
                "branches": {
                        "develop":"develop"
                }
        }
}
</pre>
'''

	/**
	 * Build config string.
	 */
	GitFlowPp() {
		JsonBuilder json = new JsonBuilder([
				['/gitflowpp/command': [
						method: 'runCommand',
						params: [
								config: ServiceParameter.PROJECT_CONFIG,
								command: ServiceParameter.LIST,
								force: ServiceParameter.ONE
						],
						description: [
								summary: 'Run arbitrary git flow commands but with support of hooks. If a hook fails the command execution, you can force to run the origin git flow command.',
								config: 'needs a working scm config and the name of your development branch as anchester point for history search',
								example: CONFIG_EXAMPLE
						],
						hooks: [
								pub: ['/scm/preMerge', '/scm/postMerge']
						],
						returns: 'git flow command output as string'
				]]
		])
		config = json.toString()
	}

	/**
	 * Run a git flow command.
	 *
	 * @param params contains the name of development branch and 'git flow ' command with switches.
	 * @return
	 */
	String runCommand(Map params) {
		assert params.config
		assert params.config.dir
		assert params.command
		assert router.hooks

		IHookManager hooks = router.hooks
		boolean force = false
		List command = params.command as List
		String dir = params.config.dir

		Map hookParams = [:]
		StringWriter out = new StringWriter()

		// check for force
		if (params.force) {
			out.println 'FORCE IS SET'
			force = true
		}

		if (command.contains('feature')) {
			if (command.contains('finish')) {
				log.debug 'found feature finish command'

				// build params
				hookParams.since = params.config.scm.branches.develop
				hookParams.until = 'HEAD'

				// pre hook and execution
				Map pre = hooks.callHook('/scm/preMerge', hookParams,
						params.config)
				if (force || (pre && pre.allOk)) {
					out.print executeGitFlowCmd(command, dir)
				} else {
					out.println 'hook "preMerge" returned with error:'
					out.println pre.error
				}

				// post hook
				hookParams.pre = pre
				hookParams.out = out.toString()
				Map post = hooks.callHook('/scm/postMerge', hookParams,
						params.config)
				if (!post) {
					return out.toString()
				} else if (force || post.allOk) {
					return post.out
				}

				out.println 'hook "postMerge" returned with error:'
				out.println post.error
				return out.toString()
			}
		}

		// execute without hooks
		executeGitFlowCmd(command, dir)
	}

	/**
	 * Helper method to execute a 'git flow' command and return the output.
	 *
	 * @param additionalCommand what comes after 'git flow'
	 * @param switches for 'git flow your command'
	 * @param dir is the working dir
	 * @return output of the constructed command
	 */
	protected String executeGitFlowCmd(List additionalCommand, String dir) {
		List command = ['git', 'flow']
		command.addAll(additionalCommand)
		log.debug "command is $command"
		ProcessBuilder builder = new ProcessBuilder(command)

		// set working dir
		builder.directory new File(dir)
		log.debug "working dir is ${builder.directory()}"

		// start process and tie it to this thread
		Process process = builder.start()
		StringBuffer out = new StringBuffer()
		StringBuffer err = new StringBuffer()
		process.waitForProcessOutput(out, err)
		"$out\n$err"
	}
}
