.PHONY: help build up down logs restart migrate revision backup shell fmt

help:
	@echo "VPN Sales Bot — available targets:"
	@echo "  make up        - build & start the whole stack (detached)"
	@echo "  make down      - stop and remove containers"
	@echo "  make logs      - follow the bot logs"
	@echo "  make restart   - restart the bot container"
	@echo "  make migrate   - apply database migrations"
	@echo "  make seed      - insert the initial plans (idempotent)"
	@echo "  make revision m=\"msg\"  - autogenerate a new migration"
	@echo "  make backup    - dump the database to ./backups"
	@echo "  make shell     - open a shell inside the bot container"

build:
	docker compose build

up:
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f bot

restart:
	docker compose restart bot

migrate:
	docker compose run --rm migrate

seed:
	docker compose run --rm bot python -m app.seed

revision:
	docker compose run --rm bot alembic revision --autogenerate -m "$(m)"

backup:
	bash scripts/backup.sh

shell:
	docker compose exec bot bash
