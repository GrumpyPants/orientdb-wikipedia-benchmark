CREATE TABLE article(
	id SERIAL PRIMARY KEY,
	title TEXT,
	links TEXT[],
	UNIQUE (title)
);

CREATE INDEX ON article ((lower(title)));
