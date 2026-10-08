using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Threading;
using System.Windows.Forms;

internal static class PortableLauncher
{
    private const string BuildId = "__PAYLOAD_SHA256__";

    [STAThread]
    private static int Main(string[] args)
    {
        string cache = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "ShutterEncoderPortable", BuildId);
        try
        {
            using (var mutex = new Mutex(false, "Local\\ShutterEncoderPortable-" + BuildId))
            {
                try { mutex.WaitOne(); }
                catch (AbandonedMutexException) { }
                try
                {
                    if (!File.Exists(Path.Combine(cache, ".ready")))
                    {
                        Directory.CreateDirectory(cache);
                        using (var resource = Assembly.GetExecutingAssembly().GetManifestResourceStream("payload.zip"))
                        using (var archive = new ZipArchive(resource, ZipArchiveMode.Read))
                        {
                            foreach (var entry in archive.Entries)
                            {
                                string destination = Path.GetFullPath(Path.Combine(cache, entry.FullName));
                                if (!destination.StartsWith(cache + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase))
                                    throw new IOException("Invalid embedded archive path.");
                                if (String.IsNullOrEmpty(entry.Name))
                                    Directory.CreateDirectory(destination);
                                else
                                {
                                    Directory.CreateDirectory(Path.GetDirectoryName(destination));
                                    entry.ExtractToFile(destination, true);
                                }
                            }
                        }
                        File.WriteAllText(Path.Combine(cache, ".ready"), BuildId);
                    }
                }
                finally { mutex.ReleaseMutex(); }
            }

            string logPath = Path.Combine(cache, "launcher.log");
            var info = new ProcessStartInfo(Path.Combine(cache, "runtime", "bin", "javaw.exe"));
            info.WorkingDirectory = cache;
            info.Arguments = "--enable-native-access=ALL-UNNAMED " +
                QuoteArgument("-Djpackage.app-path=" + Application.ExecutablePath) + " -cp \"" +
                Path.Combine(cache, "Shutter Encoder.jar") + "\" shutterencoder.ui.main.Shutter";
            foreach (string arg in args) info.Arguments += " " + QuoteArgument(arg);
            info.UseShellExecute = false;
            info.CreateNoWindow = true;
            info.RedirectStandardOutput = true;
            info.RedirectStandardError = true;
            using (var log = new StreamWriter(logPath, true))
            using (var process = new Process())
            {
                log.AutoFlush = true;
                log.WriteLine(DateTime.Now.ToString("s") + " Starting Shutter Encoder");
                object logLock = new object();
                DataReceivedEventHandler receive = delegate(object sender, DataReceivedEventArgs e)
                {
                    if (e.Data != null) lock (logLock) { log.WriteLine(e.Data); }
                };
                process.StartInfo = info;
                process.OutputDataReceived += receive;
                process.ErrorDataReceived += receive;
                process.Start();
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();
                process.WaitForExit();
                if (process.ExitCode != 0)
                    MessageBox.Show("Shutter Encoder could not run. Details: " + logPath,
                        "Shutter Encoder", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return process.ExitCode;
            }
        }
        catch (Exception e)
        {
            MessageBox.Show(e.Message, "Shutter Encoder", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }

    private static string QuoteArgument(string value)
    {
        var result = new System.Text.StringBuilder("\"");
        int backslashes = 0;
        foreach (char c in value)
        {
            if (c == '\\') { backslashes++; continue; }
            result.Append('\\', c == '"' ? backslashes * 2 + 1 : backslashes);
            result.Append(c);
            backslashes = 0;
        }
        result.Append('\\', backslashes * 2);
        return result.Append('"').ToString();
    }
}
