package org.ods.orchestration.usecase

import au.com.dius.pact.consumer.groovy.PactBuilder
import groovy.util.logging.Slf4j
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ods.core.test.usecase.LevaDocUseCaseFactory
import org.ods.core.test.usecase.RepoDataBuilder
import org.ods.core.test.usecase.levadoc.fixture.*
import org.ods.orchestration.util.Project
import org.ods.services.BitbucketService
import org.ods.services.GitService
import org.ods.services.JenkinsService
import org.ods.services.OpenShiftService
import spock.lang.Specification
import spock.lang.Unroll
import util.FixtureHelper

/**
 * Creates Consumer Contract Testing and validate LeVADocumentUse
 *
 * The generated contract is in target/pacts/ and if you change the contract
 *  you should copy it to https://github.com/opendevstack/ods-document-generation-svc
 *  path: src/test/resources/pacts/
 *
 * TIP:
 *  BUILD_ID: The current build id, such as "2005-08-22_23-59-59" (YYYY-MM-DD_hh-mm-ss)
 *  BUILD_NUMBER: The current build number, such as `153`
 *
 */

@Slf4j
class LeVADocumentUseCasePactSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder

    LevaDocWiremock levaDocWiremock

    def cleanup() {
        levaDocWiremock?.tearDownWiremock()
    }

    @Unroll
    def "docType:#projectFixture.docType (docType with default params)"() {
        given:
        String docTypeGroup = "defaultParams"
        String docType = projectFixture.getDocType()
        Map projectDataMap = projectFixtureToProjectDataMap(projectFixture)

        LevaDocUseCaseFactory levaDocUseCaseFactory = getLevaDocUseCaseFactory(projectFixture)
        LeVADocumentUseCase useCase = levaDocUseCaseFactory.build(projectFixture)

        expect: "Build the contract and execute against the generated wiremock"
        new PactBuilder()
            .with {
                serviceConsumer "buildDocument.${docTypeGroup}"
                hasPactWith "createDoc.${docTypeGroup}"
                given("project with data:", projectDataMap)
                uponReceiving("a request for /buildDocument ${docType}")
                withAttributes(method: 'post', path: "/levaDoc/${projectDataMap.project}/${projectDataMap.buildNumber}/${docType}")
                withBody([prettyPrint: true], defaultBodyParams(levaDocUseCaseFactory.getProject()))
                willRespondWith(status: 200, headers: ['Content-Type': 'application/json'])
                withBody([prettyPrint: true], defaultDocGenerationResponse())
                runTestAndVerify { context ->
                    String wiremockURL = context.url as String
                    useCase.createDocument("${projectFixture.docType}")
                }
            }

        where:
        projectFixture << new DocTypeProjectFixture().getProjects()
    }

    @Unroll
    def "docType:#projectFixture.docType (for #projectFixture.component and project: #projectFixture.project) with test data"() {
        given:
        String docTypeGroup = "testData"
        String docType = projectFixture.getDocType()
        Map projectDataMap = projectFixtureToProjectDataMap(projectFixture)

        LevaDocUseCaseFactory levaDocUseCaseFactory = getLevaDocUseCaseFactory(projectFixture)
        LeVADocumentUseCase useCase = levaDocUseCaseFactory.build(projectFixture)

        expect: "Build the contract and execute against the generated wiremock"
        new PactBuilder()
            .with {
                serviceConsumer "buildDocument.${docTypeGroup}"
                hasPactWith "createDoc.${docTypeGroup}"
                given("project with data:", projectDataMap)
                uponReceiving("a request for /buildDocument ${docType}")
                withAttributes(method: 'post', path: "/levaDoc/${projectDataMap.project}/${projectDataMap.buildNumber}/${docType}")
                withBody([prettyPrint: true], defaultBodyParams(levaDocUseCaseFactory.getProject()))
                willRespondWith(status: 200, headers: ['Content-Type': 'application/json'])
                withBody([prettyPrint: true], defaultDocGenerationResponse())
                runTestAndVerify { context ->
                    String wiremockURL = context.url as String
                    Map repoAndTestsData = getRepoAndTestsData(projectFixture, false)
                    Map repo = repoAndTestsData.repoData
                    Map data = repoAndTestsData.testsData
                    useCase.createDocument("${projectFixture.docType}", repo, data)
                }
            }

        where:
        projectFixture << new DocTypeProjectFixtureWithTestData().getProjects()
    }

    @Unroll
    def "docType:#projectFixture.docType (for component #projectFixture.component and project: #projectFixture.project) with test data and component"() {
        given:
        String docTypeGroup = "component"
        String docType = projectFixture.getDocType()
        Map projectDataMap = projectFixtureToProjectDataMap(projectFixture)

        LevaDocUseCaseFactory levaDocUseCaseFactory = getLevaDocUseCaseFactory(projectFixture)
        LeVADocumentUseCase useCase = levaDocUseCaseFactory.build(projectFixture)

        expect: "Build the contract and execute against the generated wiremock"
        new PactBuilder()
            .with {
                serviceConsumer "buildDocument.${docTypeGroup}"
                hasPactWith "createDoc.${docTypeGroup}"
                given("project with data:", projectDataMap)
                uponReceiving("a request for /buildDocument ${docType}")
                withAttributes(method: 'post', path: "/levaDoc/${projectDataMap.project}/${projectDataMap.buildNumber}/${docType}")
                withBody([prettyPrint: true], defaultBodyParamsWithComponent(levaDocUseCaseFactory.getProject(), projectFixture.getComponent()))
                willRespondWith(status: 200, headers: ['Content-Type': 'application/json'])
                withBody([prettyPrint: true], defaultDocGenerationResponse())
                runTestAndVerify { context ->
                    String wiremockURL = context.url as String
                    Map repoAndTestsData = getRepoAndTestsData(projectFixture, true)
                    Map repo = repoAndTestsData.repoData
                    Map data = repoAndTestsData.testsData
                    useCase.createDocument("${projectFixture.docType}", repo, data)
                }
            }

        where:
        projectFixture << new DocTypeProjectFixtureWithComponent().getProjects()
    }

    @Unroll
    def "docType:OVERALL_#projectFixture.docType (docType -overall- with default params)"() {
        given:
        String docTypeGroup = "overall"
        String docType = "OVERALL_${projectFixture.getDocType()}"
        Map projectDataMap = projectFixtureToProjectDataMap(projectFixture)

        LevaDocUseCaseFactory levaDocUseCaseFactory = getLevaDocUseCaseFactory(projectFixture)
        LeVADocumentUseCase useCase = levaDocUseCaseFactory.build(projectFixture)

        expect: "Build the contract and execute against the generated wiremock"
        new PactBuilder()
            .with {
                serviceConsumer "buildDocument.${docTypeGroup}"
                hasPactWith "createDoc.${docTypeGroup}"
                given("project with data:", projectDataMap)
                uponReceiving("a request for /buildDocument ${docType}")
                withAttributes(method: 'post', path: "/levaDoc/${projectDataMap.project}/${projectDataMap.buildNumber}/${docType}")
                withBody([prettyPrint: true], defaultBodyParams(levaDocUseCaseFactory.getProject()))
                willRespondWith(status: 200, headers: ['Content-Type': 'application/json'])
                withBody([prettyPrint: true], defaultDocGenerationResponse())
                runTestAndVerify { context ->
                    String wiremockURL = context.url as String
                    useCase.createDocument("OVERALL_${projectFixture.docType}")
                }
            }

        where:
        projectFixture << new DocTypeProjectFixturesOverall().getProjects()
    }

    private void executeLeVADocumentUseCaseMethod(ProjectFixture projectFixture, String wiremockURL) {
        LeVADocumentUseCase useCase = getLevaDocUseCaseFactory(projectFixture).build(projectFixture)
        useCase.createDocument("${projectFixture.docType}")
    }

    private Map getRepoAndTestsData(ProjectFixture projectFixture, boolean isForComponent) {

        Map repoData = RepoDataBuilder.getRepoForComponent(projectFixture.component)
        Map testsData = repoData.data.tests

        if (isForComponent) {
            repoData.data.remove('tests')
        }

        return [
            repoData: repoData,
            testsData: testsData,
        ]
    }

    private Closure defaultBodyParams(Project project) {
        return {
            keyLike "build", {
                targetEnvironment string("dev")
                targetEnvironmentToken string("D")
                version string("WIP")
                configItem string("BI-IT-DEVSTACK")
                changeDescription string("${project.data.build.changeDescription}")
                changeId string("${project.data.build.changeId}")
                rePromote bool(false)
                releaseStatusJiraIssueKey string("${project.data.build.releaseStatusJiraIssueKey}")
                runDisplayUrl url("${project.data.build.runDisplayUrl}")
                releaseParamVersion string("${project.data.build.releaseParamVersion}")
                buildId string("${project.data.build.buildId}")
                buildURL url("${project.data.build.buildURL}")
                jobName string("${project.data.build.jobName}")
                keyLike "testResultsURLs", {
                    'Acceptance' string("${project.data.build.testResultsURLs['Acceptance']}")
                    'Installation' string("${project.data.build.testResultsURLs['Installation']}")
                    'Integration' string("${project.data.build.testResultsURLs['Integration']}")
                    'Unit-frontend' string("${project.data.build.testResultsURLs['Unit-frontend']}")
                    'Unit-backend' string("${project.data.build.testResultsURLs['Unit-backend']}")
                }
                jenkinsLog string("${project.data.build.jenkinsLog}")
            }
            keyLike "git", {
                commit string("1e84b5100e09d9b6c5ea1b6c2ccee8957391beec")
                releaseManagerBranch string("refs/heads/master")
                releaseManagerRepo string("ordgp-releasemanager")
                baseTag string("ods-generated-v3.0-3.0-0b11-D")
                targetTag string("ods-generated-v3.0-3.0-0b11-D")
                author string("s2o")
                message string("Swingin' The Bottle")
                time string("2021-04-20T14:58:31.042152")
                // commitTime timestamp(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm.ssZZXX").pattern, "2021-04-20T14:58:31.042152")
            }
            keyLike "openshift", {
                targetApiUrl url("https://openshift-sample")
            }
        }
    }

    Closure defaultBodyParamsWithComponent(Project project, String component) {
        return {
            keyLike "repo", {
                id string("theFirst")
                type string("ods")
                keyLike "data", {
                    keyLike "openshift", {
                        keyLike "builds", {
                            keyLike "${component}", {
                                buildId string("theFirst-3")
                                image string("172.30.1.1:5000/ordgp-cd/teFirst@sha256:f6bc9aaed8a842a8e0a4f7e69b044a12c69e057333cd81906c08fd94be044ac4")
                            }
                        }
                        keyLike "deployments", {
                            keyLike "${component}", {
                                podName string("theFirst-3")
                                podNamespace string("foi2004-dev")
                                podMetaDataCreationTimestamp string("2021-11-21T22:31:04Z")
                                deploymentId string("theFirst-3")
                                podNode string("localhost")
                                podIp string("172.17.0.39")
                                podStatus string("Running")
                                podStartupTimeStamp string("2021-11-21T22:31:04Z")
                                podIp string("172.17.0.39")
                                keyLike "containers", {
                                    "${component}" string("172.30.1.1:5000/ordgp-cd/therFirst@sha256:f6bc9aaed8a842a8e0a4f7e69b044a12c69e057333cd81906c08fd94be044ac4")
                                }
                            }
                        }
                        sonarqubeScanStashPath string("scrr-report-theFirst-1")
                        'SCRR' string("SCRR-ordgp-theFirst.docx")
                        'SCRR-MD' string("SCRR-ordgp-theFirst.md")
                        testResultsFolder string("build/test-results/test")
                        testResults string("1")
                        xunitTestResultsStashPath string("test-reports-junit-xml-theFirst-1")
                        'CREATED_BY_BUILD' string("WIP/1")
                    }
                    keyLike "documents", {}
                    keyLike "git", {
                        branch string("master")
                        commit string("")
                        previousCommit nullValue()
                        previousSucessfulCommit nullValue()
                        url url("http://bitbucket.odsbox.lan:7990/scm/ordgp/ordgp-theFirst.git")
                        baseTag string("")
                        targetTag string("")
                    }
                }
                url url("http://bitbucket.odsbox.lan:7990/scm/ordgp/ordgp-theFirst.git")
                branch string("master")
                keyLike "pipelineConfig", {
                    dependencies eachLike([])
                }
                keyLike "metadata", {
                    name string("OpenJDK")
                    description string("OpenJDK is a free and open-source implementation of the Java Platform, Standard Edition. Technologies: Spring Boot 2.1, OpenJDK 11, supplier:https://adoptopenjdk.net")
                    version string("3.x")
                    type string("ods")
                }
            }
        } << defaultBodyParams(project)
    }

    Closure defaultDocGenerationResponse() {
        return {
            nexusURL eachLike(){ url("http://lalala") }
            [ tempFolder.root ]
        }
    }

    Map projectFixtureToProjectDataMap(ProjectFixture projectFixture) {
        return [
            project: projectFixture.getProject(),
            buildNumber: projectFixture.getBuildNumber(),
            version: projectFixture.getVersion(),
            docType: projectFixture.getDocType(),
        ]
    }

    private LevaDocUseCaseFactory getLevaDocUseCaseFactory(ProjectFixture projectFixture) {
        levaDocWiremock = new LevaDocWiremock()
        levaDocWiremock.setUpWireMock(projectFixture, tempFolder.root)

        // Mocks generation (spock don't let you add this outside a Spec)
        JenkinsService jenkins = Mock(JenkinsService)
        jenkins.unstashFilesIntoPath(_, _, _) >> true
        jenkins.getCurrentBuildLogInputStream() >> new ByteArrayInputStream()
        OpenShiftService openShiftService = Mock(OpenShiftService)
        GitService gitService = Mock(GitService)
        BitbucketService bitbucketService = Mock(BitbucketService)
        BitbucketTraceabilityUseCase bbT = Spy(new BitbucketTraceabilityUseCase(bitbucketService, null, null))
        bbT.generateSourceCodeReviewFile() >> new FixtureHelper()
            .getResource(BitbucketTraceabilityUseCaseSpec.EXPECTED_BITBUCKET_CSV).getAbsolutePath()

        return new LevaDocUseCaseFactory(
            levaDocWiremock,
            gitService,
            tempFolder,
            jenkins,
            openShiftService,
            bbT,
            bitbucketService)
    }

}
