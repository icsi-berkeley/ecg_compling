package compling.gui.grammargui.util

import java.util.Formatter
import org.eclipse.core.runtime.IStatus
import compling.gui.grammargui.EcgEditorPlugin
import org.eclipse.core.runtime.Status
import scala.annotation.varargs

object Log2 {
   val formatter = new Formatter
   def format(fmt: String, args: Object*) = formatter.format(fmt, args)

   def formatString(fmt: String, args: Object*) = format(fmt, args) toString ()

   val log = EcgEditorPlugin.getDefault().getLog().log(_)

   def logInfo(fmt: String, args: Object*) =
      log(makeStatus(IStatus.INFO, IStatus.OK, formatString(fmt, args), null));

   def logError(t: Throwable) =
      log(makeStatus(IStatus ERROR, IStatus OK, "Unexpected exception", t))

   def logError(t: Throwable, fmt: String, args: Object*) =
      log(makeStatus(IStatus ERROR, IStatus OK, formatString(fmt, args), t))

   def makeStatus(severity: Int, code: Int, message: String, t: Throwable) =
      new Status(severity, EcgEditorPlugin PLUGIN_ID, code, message, t);

   @varargs
   def consoleLog(fmt: String, args: Object*) = println(formatString(fmt, args))
   //	def consoleLog(fmt: String, args: Object*): Unit = println(formatString(fmt, args))
}