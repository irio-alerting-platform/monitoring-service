# Setup services

In the project root directory run:

- `sbt docker:stage` this will create a `Dockerfile` in `target/docker/stage` directory
- `docker build -t irio-monitoring-service target/docker/stage`

You should provide a file with the following environment variables:
- `MAILER_HOST` server hosting mailing service
- `MAILER_PORT` port for outgoing emails, usually `587`
- `MAILER_FROM` login to email account for sending emails
- `MAILER_PASS` password to email account

# Run services

Run services in Docker:

`docker run --env-file=<env_file> irio-monitoring-service`

