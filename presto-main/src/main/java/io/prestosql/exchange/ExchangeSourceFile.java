/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.exchange;

import javax.annotation.concurrent.Immutable;
import javax.crypto.SecretKey;

import java.net.URI;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Immutable
public class ExchangeSourceFile
{
    private final URI fileUri;
    private final Optional<SecretKey> secretKey;
    private final boolean exchangeCompressionEnabled;
    private final long fileSize;

    public ExchangeSourceFile(URI fileUri, Optional<SecretKey> secretKey, boolean exchangeCompressionEnabled, long fileSize)
    {
        this.fileUri = requireNonNull(fileUri, "fileUri is null");
        this.secretKey = requireNonNull(secretKey, "secretKey is null");
        this.exchangeCompressionEnabled = exchangeCompressionEnabled;
        this.fileSize = fileSize;
    }

    public URI getFileUri()
    {
        return fileUri;
    }

    public Optional<SecretKey> getSecretKey()
    {
        return secretKey;
    }

    public boolean isExchangeCompressionEnabled()
    {
        return exchangeCompressionEnabled;
    }

    public long getFileSize()
    {
        return fileSize;
    }
}
