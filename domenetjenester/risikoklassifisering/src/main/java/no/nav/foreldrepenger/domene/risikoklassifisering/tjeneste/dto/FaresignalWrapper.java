package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;

import jakarta.validation.constraints.NotNull;

public record FaresignalWrapper(@NotNull Kontrollresultat kontrollresultat,
                                FaresignalVurdering faresignalVurdering,
                                FaresignalGruppeWrapper medlemskapFaresignaler,
                                FaresignalGruppeWrapper iayFaresignaler) {}


