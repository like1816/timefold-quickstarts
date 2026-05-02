package org.acme.vehiclerouting.benchmark;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import org.acme.vehiclerouting.app.SolutionBuilder;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * autoresearch 的主入口类，用于运行 benchmark 并记录结果
 * 
 * 主要功能：
 * 1. 运行 Timefold Benchmark
 * 2. 解析 benchmark 结果（XML + CSV）
 * 3. 记录到 results.tsv 用于后续对比
 */
public class VehicleRoutingBenchmarkRunner {

    // 结果文件名称，TSV 格式
    private static final String RESULTS_TSV = "results.tsv";
    // Git commit 取前 7 位
    private static final int GIT_COMMIT_LENGTH = 7;
    // 报告目录时间戳格式
    private static final DateTimeFormatter DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    /**
     * 主方法：运行完整的 benchmark 流程
     * 
     * @param args 可选参数，指定问题实例（默认是 r101）
     */
    public static void main(String[] args) {
        // 1. 获取问题实例名称（默认 r101）
        String problem = args.length > 0 ? args[0] : "r101";

        // 2. 生成唯一的报告目录名（基于当前时间戳）
        String timestamp = LocalDateTime.now().format(DIR_FORMATTER);
        String reportDir = "local/benchmarkReport/" + timestamp;

        // 3. 从 XML 配置文件加载配置，并修改报告目录
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromXmlResource(
                "vehicleRoutingBenchmarkConfig.xml");
        benchmarkConfig.setBenchmarkDirectory(new File(reportDir));

        // 4. 创建 BenchmarkFactory 并构建 Benchmark
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.create(benchmarkConfig);
        VehicleRoutePlan problemSolution = SolutionBuilder.buildFromSolomon(problem);
        PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark(problemSolution);

        // 5. 运行 Benchmark
        benchmark.benchmark();

        // 6. 获取当前 Git commit 哈希
        String gitCommit = getGitCommit();

        // 7. 解析结果并记录到 results.tsv（直接用我们刚才指定的目录）
        parseAndLogResults(reportDir, gitCommit, problem);

        // 8. 打印完成信息
        System.out.println("Benchmark completed! Report in " + reportDir);
        System.out.println("Results logged to: " + RESULTS_TSV);
    }

    /**
     * 获取当前 Git commit 的短哈希（7位）
     * 
     * @return Git commit 哈希，如果获取失败则返回 "unknown"
     */
    private static String getGitCommit() {
        try {
            // 运行 git 命令获取短 commit
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--short", "HEAD");
            pb.directory(new File(".").getCanonicalFile());
            Process process = pb.start();
            String commit = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();
            if (exitCode == 0 && commit.length() >= GIT_COMMIT_LENGTH) {
                return commit.substring(0, GIT_COMMIT_LENGTH);
            }
        } catch (Exception e) {
            System.err.println("Error getting git commit: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * 解析 benchmark 结果并记录到 results.tsv
     * 
     * @param reportDir 报告目录路径
     * @param gitCommit Git commit 哈希
     * @param problem 问题实例名称
     */
    private static void parseAndLogResults(String reportDir, String gitCommit, String problem) {
        // 1. 检查 reportDir 下面是否有子目录，Timefold 可能会再包一层
        File baseDir = new File(reportDir);
        String actualReportDir = reportDir;
        
        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] subDirs = baseDir.listFiles(File::isDirectory);
            if (subDirs != null && subDirs.length > 0) {
                actualReportDir = subDirs[0].getPath();
            }
        }

        // 2. 检查 XML 结果文件是否存在
        File resultXml = new File(actualReportDir, "plannerBenchmarkResult.xml");
        if (!resultXml.exists()) {
            System.err.println("Result XML not found: " + resultXml.getPath());
            return;
        }

        try {
            // 2. 解析 XML 文档
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(resultXml);
            doc.getDocumentElement().normalize();

            // 3. 获取 solver 配置的结果（我们只用第一个配置）
            NodeList solverBenchmarkResults = doc.getElementsByTagName("solverBenchmarkResult");
            if (solverBenchmarkResults.getLength() == 0) {
                System.err.println("No config found!");
                return;
            }

            // 4. 解析单个配置的详细结果
            ConfigResult config = parseConfigResult((Element) solverBenchmarkResults.item(0), actualReportDir);

            // 5. 获取整个 benchmark 的总运行时间
            Element root = doc.getDocumentElement();
            String benchmarkTime = root.getElementsByTagName("benchmarkTimeMillisSpent").item(0).getTextContent();

            // 6. 追加结果到 TSV 文件
            appendResult(gitCommit, problem, benchmarkTime, config, actualReportDir);

        } catch (Exception e) {
            System.err.println("Error parsing results: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 解析单个 solver 配置的详细结果
     * 
     * @param solverResult XML 中的 solverBenchmarkResult 元素
     * @param reportDir 报告目录路径
     * @return 解析后的配置结果对象
     */
    private static ConfigResult parseConfigResult(Element solverResult, String reportDir) {
        ConfigResult result = new ConfigResult();
        // 获取配置名称
        result.name = solverResult.getElementsByTagName("name").item(0).getTextContent();

        // 获取子运行结果（subSingleBenchmarkResult）
        NodeList subResults = solverResult.getElementsByTagName("subSingleBenchmarkResult");
        if (subResults.getLength() == 0) {
            result.succeeded = false;
            return result;
        }

        Element subResult = (Element) subResults.item(0);
        // 检查运行是否成功
        result.succeeded = Boolean.parseBoolean(subResult.getElementsByTagName("succeeded").item(0).getTextContent());

        if (result.succeeded) {
            // 解析最终分数（完整的 hard/medium/soft）
            Element scoreElement = (Element) subResult.getElementsByTagName("score").item(0);
            if (scoreElement != null) {
                result.finalScoreStr = scoreElement.getTextContent();
                HardMediumSoftScore score = HardMediumSoftScore.parseScore(result.finalScoreStr);
                result.hardScore = score.hardScore();
                result.mediumScore = score.mediumScore();
                result.softScore = score.softScore();
            }
            // 解析运行时间、分数计算次数、move 评估次数
            result.timeSpent = subResult.getElementsByTagName("timeMillisSpent").item(0).getTextContent();
            result.scoreCalcCount = subResult.getElementsByTagName("scoreCalculationCount").item(0).getTextContent();
            result.moveEvalCount = subResult.getElementsByTagName("moveEvaluationCount").item(0).getTextContent();

            // 从 CSV 文件解析收敛数据（初始分数、收敛时间）
            String csvPath = reportDir + "/Problem_0/" + result.name + "/sub0/BEST_SCORE.csv";
            parseConvergenceData(csvPath, result);
        }

        return result;
    }

    /**
     * 从 BEST_SCORE.csv 解析收敛数据（初始分数、最后一次改进的时间）
     * 
     * @param csvPath CSV 文件路径
     * @param result 结果对象，将解析到的数据填充进去
     */
    private static void parseConvergenceData(String csvPath, ConfigResult result) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            List<Double> scores = new ArrayList<>();
            List<Integer> times = new ArrayList<>();

            br.readLine(); // 跳过表头
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    times.add(Integer.parseInt(parts[0])); // 第一列是时间（毫秒）
                    String scorePart = parts[1].replace("\"", "");
                    HardMediumSoftScore score = HardMediumSoftScore.parseScore(scorePart);
                    scores.add((double) score.softScore()); // 第二列是分数
                }
            }

            // 提取：第一行是初始分数，最后一行是最后一次改进的时间
            if (!scores.isEmpty()) {
                result.initialScore = scores.get(0);
                result.convergenceTimeMs = times.get(times.size() - 1);
            }

        } catch (Exception e) {
            System.err.println("Error parsing CSV: " + e.getMessage());
        }
    }

    /**
     * 追加一条结果到 results.tsv 文件（如果文件不存在则先创建表头）
     * 
     * @param commit Git commit 哈希
     * @param problem 问题实例名称
     * @param benchmarkTime benchmark 总运行时间
     * @param config 配置结果对象
     * @param reportDir 报告目录路径
     */
    private static synchronized void appendResult(String commit, String problem, String benchmarkTime,
                                                  ConfigResult config, String reportDir) {
        Path tsvPath = Paths.get(RESULTS_TSV);
        boolean fileExists = Files.exists(tsvPath);

        try (FileWriter writer = new FileWriter(tsvPath.toFile(), true)) {
            // 如果文件不存在，先写表头
            if (!fileExists) {
                writer.write("commit\tproblem\tbenchmark_time\tconfig_name\t" +
                        "final_score\trun_time_ms\tscore_calc_count\tmove_eval_count\t" +
                        "initial_score\tconvergence_time_ms\tstatus\treport_dir\n");
            }

            // 构建结果行
            String status = config.succeeded ? "success" : "crash";
            String finalScore = config.succeeded ? config.finalScoreStr : "N/A";
            String line = String.format(
                    "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%.0f\t%d\t%s\t%s\n",
                    commit, problem, benchmarkTime, config.name, finalScore, config.timeSpent,
                    config.scoreCalcCount, config.moveEvalCount, config.initialScore,
                    config.convergenceTimeMs, status, reportDir
            );

            // 写入文件并打印到控制台
            writer.write(line);
            System.out.println("Logged: " + line.trim());

        } catch (IOException e) {
            System.err.println("Error writing results: " + e.getMessage());
        }
    }

    /**
     * 内部类，用于存储单个配置的 benchmark 结果
     */
    static class ConfigResult {
        String name;
        boolean succeeded = false;
        long hardScore = 0;
        long mediumScore = 0;
        long softScore = 0;
        String finalScoreStr = "";
        String timeSpent = "0";
        String scoreCalcCount = "0";
        String moveEvalCount = "0";
        double initialScore = 0.0;
        int convergenceTimeMs = 0;
    }
}
