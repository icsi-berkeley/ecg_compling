# compling
Repository for "compling" files - ECG Analyzer, ECG Workbench, and dependencies.
Use Eclipse Neon RCP to compile the Analyzer and Workbench. Compiled copies of the
Analyzer and Workbench can be found in the [ecg_framework_code](https://github.com/icsi-berkeley/ecg_framework_code)
and [ecg_workbench_release](https://github.com/icsi-berkeley/ecg_workbench_release)
repositories respectively. 


### Compilation Instructions
1. Download Eclipse RCP Oxygen 4.7
2. Download compling dev branch
3. Import compling from git using eclipse
4. Go to preferences->plug in development->api baselines and set it from error to warning
5. Go to file export and java->JAR file for the **Analyzer jar**
6. Go to file export and plug-in development->eclipse product file for the **Workbench**

Note: That in order to export the eclipse product for multiple platforms, you need to follow the instructions given [here](https://wiki.eclipse.org/A_Brief_Overview_of_Building_at_Eclipse#Multi-platform_builds).
