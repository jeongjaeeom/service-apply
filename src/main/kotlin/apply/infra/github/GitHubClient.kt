package apply.infra.github

import apply.domain.judgment.AssignmentArchive
import apply.domain.judgment.Commit
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import support.toUri
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class GitHubClient(
    private val gitHubProperties: GitHubProperties,
    restTemplateBuilder: RestTemplateBuilder
) : AssignmentArchive {
    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    /**
     * @see [API](https://docs.github.com/en/rest/pulls/pulls#list-commits-on-a-pull-request)
     */
    override fun getLastCommit(pullRequestUrl: String, endDateTime: LocalDateTime): Commit {
        val (owner, repo, pullNumber) = extract(pullRequestUrl)
        val requestEntity = RequestEntity
            .get("${gitHubProperties.uri}/repos/$owner/$repo/pulls/$pullNumber/commits?per_page=$PAGE_SIZE".toUri())
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(gitHubProperties.accessKey))
            .build()
        val zonedDateTime = endDateTime.atZone(ZoneId.systemDefault())
        return restTemplate.exchange<List<CommitResponse>>(requestEntity).body
            ?.filter { it.date <= zonedDateTime }
            ?.maxByOrNull { it.date }
            ?.let { Commit(it.hash) }
            ?: throw IllegalArgumentException("해당 커밋이 존재하지 않습니다. endDateTime: $endDateTime")
    }

    private fun extract(pullRequestUrl: String): List<String> {
        val result = PULL_REQUEST_URL_PATTERN.find(pullRequestUrl)
            ?: throw IllegalArgumentException("올바른 형식의 URL이어야 합니다")
        return result.destructured.toList()
    }

    private fun bearerToken(token: String): String = if (token.isEmpty()) "" else "Bearer $token"

    companion object {
        private const val PAGE_SIZE: Int = 100
        private val PULL_REQUEST_URL_PATTERN: Regex =
            "https://github\\.com/(?<owner>.+)/(?<repo>.+)/pull/(?<pullNumber>\\d+)".toRegex()
    }
}