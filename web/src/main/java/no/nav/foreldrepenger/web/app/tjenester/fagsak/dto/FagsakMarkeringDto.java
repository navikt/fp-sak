package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;

public record FagsakMarkeringDto(@NotNull FagsakMarkering fagsakMarkering, @NotNull String kortNavn) { }
