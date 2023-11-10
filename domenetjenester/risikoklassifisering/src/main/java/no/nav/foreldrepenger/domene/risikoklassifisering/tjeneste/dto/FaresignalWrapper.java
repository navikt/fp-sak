package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;

public record FaresignalWrapper(@NotNull Kontrollresultat kontrollresultat,
                                FaresignalVurdering faresignalVurdering,
                                FaresignalGruppeWrapper medlemskapFaresignaler,
                                FaresignalGruppeWrapper iayFaresignaler) {}


