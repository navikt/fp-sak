package no.nav.foreldrepenger.domene.abakus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;

import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

@RequestScoped
class IAYRequestCache {
    private final List<InntektArbeidYtelseGrunnlag> cacheGrunnlag = new ArrayList<>();

    void leggTil(InntektArbeidYtelseGrunnlag dto) {
        this.cacheGrunnlag.removeIf(g -> dto.getEksternReferanse().equals(g.getEksternReferanse()));
        this.cacheGrunnlag.add(dto);
    }

    InntektArbeidYtelseGrunnlag getGrunnlag(UUID grunnlagReferanse) {
        if (grunnlagReferanse == null) {
            return null;
        }
        return this.cacheGrunnlag.stream().filter(g -> Objects.equals(g.getEksternReferanse(), grunnlagReferanse)).findFirst().orElse(null);
    }

    UUID getSisteAktiveGrunnlagReferanse(UUID behandlingUuid) {
        return this.cacheGrunnlag.stream()
                .filter(it -> behandlingUuid.equals(it.getKoblingReferanse().orElse(null)))
                .filter(InntektArbeidYtelseGrunnlag::isAktiv).max(Comparator.comparing(InntektArbeidYtelseGrunnlag::getOpprettetTidspunkt))
                .map(InntektArbeidYtelseGrunnlag::getEksternReferanse)
                .orElse(null);
    }

}
