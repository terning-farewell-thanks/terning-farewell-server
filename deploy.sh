#!/bin/bash
set -e

echo "Pulling latest docker image..."
docker pull jsoonworld/terning-farewell-server:latest

echo "Stopping and removing existing containers..."
docker compose down

echo "Starting new containers..."
docker compose up -d

echo "Cleaning up dangling images..."
docker image prune -f

echo "Deployment completed successfully!"
