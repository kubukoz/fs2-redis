/*
 * Copyright 2018 Fs2 Redis
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

package com.github.gvolpe.fs2redis

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.all._
import com.github.gvolpe.fs2redis.algebra.GeoCommands
import com.github.gvolpe.fs2redis.interpreter.Fs2Redis
import com.github.gvolpe.fs2redis.interpreter.connection.Fs2RedisClient
import com.github.gvolpe.fs2redis.model._
import io.lettuce.core.GeoArgs

object Fs2RedisGeoDemo extends IOApp {

  import Demo._

  override def run(args: List[String]): IO[ExitCode] = {
    val testKey = "location"

    val commandsApi: Resource[IO, GeoCommands[IO, String, String]] =
      for {
        client <- Fs2RedisClient[IO](redisURI)
        redis  <- Fs2Redis[IO, String, String](client, stringCodec, redisURI)
      } yield redis

    val _BuenosAires  = GeoLocation(Longitude(-58.3816), Latitude(-34.6037), "Buenos Aires")
    val _RioDeJaneiro = GeoLocation(Longitude(-43.1729), Latitude(-22.9068), "Rio de Janeiro")
    val _Montevideo   = GeoLocation(Longitude(-56.164532), Latitude(-34.901112), "Montevideo")
    val _Tokyo        = GeoLocation(Longitude(139.6917), Latitude(35.6895), "Tokyo")

    commandsApi.use { cmd =>
      for {
        _ <- cmd.geoAdd(testKey, _BuenosAires)
        _ <- cmd.geoAdd(testKey, _RioDeJaneiro)
        _ <- cmd.geoAdd(testKey, _Montevideo)
        _ <- cmd.geoAdd(testKey, _Tokyo)
        x <- cmd.geoDist(testKey, _BuenosAires.value, _Tokyo.value, GeoArgs.Unit.km)
        _ <- putStrLn(s"Distance from ${_BuenosAires.value} to Tokyo: $x km")
        y <- cmd.geoPos(testKey, _RioDeJaneiro.value)
        _ <- putStrLn(s"Geo Pos of ${_RioDeJaneiro.value}: ${y.headOption}")
        z <- cmd.geoRadius(testKey, GeoRadius(_Montevideo.lon, _Montevideo.lat, Distance(10000.0)), GeoArgs.Unit.km)
        _ <- putStrLn(s"Geo Radius in 1000 km: $z")
      } yield ()
    } *> IO.pure(ExitCode.Success)
  }

}
