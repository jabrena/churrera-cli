package info.jab.churrera.cli.service;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.BindResultTypeMapper;
import info.jab.churrera.util.ConversationJsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service for extracting and deserializing results from parallel workflow conversations.
 */
public class ResultExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ResultExtractor.class);

    private final CLIAgent cliAgent;
    private final JobRepository jobRepository;

    public ResultExtractor(CLIAgent cliAgent, JobRepository jobRepository) {
        this.cliAgent = cliAgent;
        this.jobRepository = jobRepository;
    }

    /**
     * Extract and deserialize results from a parallel workflow conversation.
     *
     * @param job the parent job
     * @param parallelData the parallel workflow data
     * @return the deserialized list of results, or null if extraction failed
     */
    public List<Object> extractResults(Job job, ParallelWorkflowData parallelData) {
        try {
            logger.info("Parent job {} completed successfully, attempting to extract results from conversation", job.jobId());

            // Get conversation to extract result
            String conversationContent = cliAgent.getConversationContent(job.cursorAgentId());
            logger.info("Conversation content length: {} characters", conversationContent.length());

            // Deserialize results based on bindResultType
            String jsonResult = null;
            List<Object> deserializedList = null;

            if (parallelData.hasBindResultType()) {
                String bindResultType = parallelData.getBindResultType();
                logger.info("Attempting to deserialize result with type: {}", bindResultType);

                if (BindResultTypeMapper.isListType(bindResultType)) {
                    // Deserialize to list
                    Class<?> elementType = BindResultTypeMapper.mapToElementType(bindResultType);

                    // Use raw type to handle generics, passing bindResultType as preferred key
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Optional<List<Object>> resultList =
                        (Optional<List<Object>>) (Optional)
                        ConversationJsonDeserializer.deserializeList(conversationContent, (Class) elementType, bindResultType);

                    if (resultList.isPresent()) {
                        deserializedList = resultList.get();
                        logger.info("Successfully deserialized {} elements from conversation", deserializedList.size());
                        // Store as proper JSON array using Jackson ObjectMapper
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            jsonResult = mapper.writeValueAsString(deserializedList);
                            logger.info("Successfully serialized result as JSON: {}", jsonResult);
                        } catch (JsonProcessingException e) {
                            // Fallback to toString if JSON serialization fails
                            jsonResult = deserializedList.toString();
                            logger.error("Failed to serialize result as JSON, using toString: {}", e.getMessage());
                        }
                    } else {
                        logger.error("Failed to deserialize result from conversation for job: {}", job.jobId());
                        logger.error("Full conversation content for failed deserialization (length: {} chars): {}",
                            conversationContent.length(), conversationContent);
                    }
                }
            }

            // Store extracted JSON result in job (not the full conversation)
            if (jsonResult != null) {
                job = job.withResult(jsonResult);
                jobRepository.save(job);
                logger.info("Successfully stored result for parent job: {}", job.jobId());
                return deserializedList;
            } else {
                logger.error("No result to store for parent job: {}", job.jobId());
                // If deserialization failed after a successful agent run, mark job as FAILED
                return List.of();
            }

        } catch (Exception e) {
            logger.error("Error extracting results for job {}: {}", job.jobId(), e.getMessage(), e);
            return List.of();
        }
    }
}

