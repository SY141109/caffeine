/*
 * Copyright 2014 Ben Manes. All Rights Reserved.
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
package com.github.benmanes.caffeine.cache.testing;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import org.testng.annotations.DataProvider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Iterators;

/**
 * A data provider that generates caches based on the {@link CacheSpec} configuration.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class CacheProvider {

  private CacheProvider() {}

  /**
   * The provided test parameters are optional and may be specified in any order. Supports injecting
   * {@link LoadingCache}, {@link Cache}, {@link CacheContext}, and the {@link ConcurrentMap}
   * {@link Cache#asMap()} view.
   */
  @DataProvider(name = "caches")
  public static Iterator<Object[]> providesCaches(Method testMethod) throws Exception {
    CacheSpec cacheSpec = testMethod.getAnnotation(CacheSpec.class);
    requireNonNull(cacheSpec, "@CacheSpec not found");

    boolean isLoadingOnly = hasLoadingCache(testMethod);
    CacheGenerator generator = new CacheGenerator(cacheSpec, isLoadingOnly);
    return asTestCases(testMethod, generator.generate());
  }

  /** Converts each scenario into test case parameters. */
  private static Iterator<Object[]> asTestCases(Method testMethod,
      Iterator<Entry<CacheContext, Cache<Integer, Integer>>> scenarios) {
    Class<?>[] parameterClasses = testMethod.getParameterTypes();
    return Iterators.transform(scenarios, entry -> {
      Object[] params = new Object[parameterClasses.length];
      for (int i = 0; i < params.length; i++) {
        if (parameterClasses[i].isAssignableFrom(entry.getKey().getClass())) {
          params[i] = entry.getKey();
        } else if (parameterClasses[i].isAssignableFrom(entry.getValue().getClass())) {
          params[i] = entry.getValue();
        } else if (parameterClasses[i].isAssignableFrom(Map.class)) {
          params[i] = entry.getValue().asMap();
        } else {
          throw new AssertionError("Unknown parameter type: " + parameterClasses[i]);
        }
      }
      return params;
    });
  }

  private static boolean hasLoadingCache(Method testMethod) throws Exception {
    for (Class<?> param : testMethod.getParameterTypes()) {
      if (LoadingCache.class.isAssignableFrom(param)) {
        return true;
      }
    }
    return false;
  }
}