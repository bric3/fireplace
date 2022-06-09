/*
 * Copyright 2021 Datadog, Inc.
 *
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

package io.github.bric3.fireplace.flamegraph;

/**
 * Backward compatibility class, use {@link FlamegraphView} instead.
 * @param <T> The type of frame node
 */
@Deprecated(forRemoval = true)
public class FlameGraph<T> extends FlamegraphView<T> {

    @Deprecated(forRemoval = true)
    public interface ZoomAction extends FlamegraphView.ZoomAction {}
    
    @Deprecated(forRemoval = true)
    public interface HoveringListener<T> extends FlamegraphView.HoveringListener<T> {}
}
