# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IM工单系统（IM Ticket System）— a multi-channel customer service ticketing system integrating WeCom (企业微信) and Feishu (飞书) IM channels. Currently in design phase; implementation has not started.

The primary artifact is the system design spec at `docs/superpowers/specs/2026-05-26-im-ticket-system-design.md`.

## Target Tech Stack (per spec)

- **Backend**: Java 17+, Spring Boot 3.x, Spring AI, Spring StateMachine, RabbitMQ, MySQL 8.x, Redis, Elasticsearch
- **Frontend**: Vue 3 + Element Plus + Pinia, WebSocket (STOMP)
- **Deployment**: Nginx + multi-instance

## Key Design Decisions

- **Smart routing**: Rule engine (fast path) → LLM intent recognition (fallback) → default ticket creation (degraded)
- **Knowledge base**: Three-tier retrieval — FAQ (MySQL exact match) → Document RAG (ES vector) → Historical tickets (ES vector)
- **Ticket state machine**: 8 states with deterministic single-step transitions (no workflow engine needed)
- **Notifications**: Dual-channel — internal (workbench push + IM Bot) and external (reply to source IM channel)
- **Channel abstraction**: Adapter pattern — unified internal message format with per-channel adapters