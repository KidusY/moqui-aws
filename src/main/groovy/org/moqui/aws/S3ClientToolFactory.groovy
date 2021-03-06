/*
 * This software is in the public domain under CC0 1.0 Universal plus a 
 * Grant of Patent License.
 * 
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 * 
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software (see the LICENSE.md file). If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package org.moqui.aws

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import groovy.transform.CompileStatic
import org.moqui.context.ExecutionContextFactory
import org.moqui.context.ToolFactory
import org.moqui.util.SystemBinding
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** A ToolFactory for AWS S3 Client */
@CompileStatic
class S3ClientToolFactory implements ToolFactory<AmazonS3> {
    protected final static Logger logger = LoggerFactory.getLogger(S3ClientToolFactory.class)
    final static String TOOL_NAME = "AwsS3Client"

    protected ExecutionContextFactory ecf = null
    protected AmazonS3 s3Client = null

    /** Default empty constructor */
    S3ClientToolFactory() { }

    @Override String getName() { return TOOL_NAME }

    @Override
    void init(ExecutionContextFactory ecf) {
        this.ecf = ecf
        // NOTE: minimal explicit configuration here, see:
        // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
        // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html

        // There is no Java sys prop key for region, and env var vs Java sys prop keys are different for access key ID and secret
        //     so normalize here to the standard SDK env var keys and support from Java sys props as well
        String awsRegion = SystemBinding.getPropOrEnv("AWS_REGION")
        String awsAccessKeyId = SystemBinding.getPropOrEnv("AWS_ACCESS_KEY_ID")
        String awsSecret = SystemBinding.getPropOrEnv("AWS_SECRET_ACCESS_KEY")
        if (awsAccessKeyId && awsSecret) {
            System.setProperty("aws.accessKeyId", awsAccessKeyId)
            System.setProperty("aws.secretKey", awsSecret)
        }

        logger.info("Starting AWS S3 Client with region ${awsRegion} access ID ${awsAccessKeyId}")

        AmazonS3ClientBuilder cb = AmazonS3ClientBuilder.standard()
        if (awsRegion) cb.withRegion(awsRegion)
        s3Client = cb.build()
    }

    @Override AmazonS3 getInstance(Object... parameters) { return s3Client }

    @Override
    void destroy() {
        // stop Camel to prevent more calls coming in
        if (s3Client != null) try {
            s3Client.shutdown()
            logger.info("AWS S3 Client shut down")
        } catch (Throwable t) { logger.error("Error in AWS S3 Client shut down", t) }
    }
}
