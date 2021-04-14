package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingFilterYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class InntektsmeldingFilterYtelseImpl implements InntektsmeldingFilterYtelse {

    private SvangerskapspengerRepository svangerskapspengerRepository;

    @Inject
    public InntektsmeldingFilterYtelseImpl(SvangerskapspengerRepository svangerskapspengerRepository) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
    }

    public InntektsmeldingFilterYtelseImpl() {
        // Jepp...
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse,
            Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
            Map<Arbeidsgiver, Set<V>> påkrevde) {
        Map<Arbeidsgiver, Set<V>> filtrert = new HashMap<>();

        var arbeidsforholdFraSøknad = svangerskapspengerRepository.hentGrunnlag(referanse.getBehandlingId())
                .map(svpGrunnlagEntitet -> new TilretteleggingFilter(svpGrunnlagEntitet).getAktuelleTilretteleggingerUfiltrert())
                .orElse(Collections.emptyList());

        påkrevde.forEach((key, value) -> {
            if (arbeidsforholdFraSøknad.stream()
                    .anyMatch(trlg -> trlg.getArbeidsgiver().map(arbeidsgiver -> arbeidsgiver.equals(key)).orElse(false))) {
                filtrert.put(key, value);
            }
        });
        return filtrert;
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse,
            Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
            Map<Arbeidsgiver, Set<V>> påkrevde) {
        return påkrevde;
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForKompletthetAktive(BehandlingReferanse referanse,
            Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
            Map<Arbeidsgiver, Set<V>> påkrevde) {
        return filtrerInntektsmeldingerForYtelse(referanse, inntektArbeidYtelseGrunnlag, påkrevde);
    }
}
