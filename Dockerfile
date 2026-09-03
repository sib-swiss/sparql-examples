FROM docker.io/jekyll/jekyll:latest

WORKDIR /srv/jekyll

COPY Gemfile Gemfile.lock ./
RUN bundle install

ENTRYPOINT ["bundle", "exec"]
CMD ["jekyll", "build"]
