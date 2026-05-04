FROM ghcr.io/rpjvanaert/hackathon-chessengine-base:latest

WORKDIR /app

COPY sprt_presets.ini /app/sprt_presets.ini
COPY 8moves_v3.pgn /app/8moves_v3.pgn
COPY cutechess-cli /app/cutechess-cli

COPY test_sprt.sh /app/test_sprt.sh
RUN chmod +x /app/test_sprt.sh

COPY engines /app/engines

ENTRYPOINT ["/app/test_sprt.sh"]