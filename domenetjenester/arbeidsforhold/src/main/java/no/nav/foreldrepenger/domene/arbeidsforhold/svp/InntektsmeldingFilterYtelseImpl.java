package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
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
        //Jepp...
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse,
                                                                           Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                           Map<Arbeidsgiver, Set<V>> påkrevde) {
        Map<Arbeidsgiver, Set<V>> filtrert = new HashMap<>();

        List<SvpTilretteleggingEntitet> arbeidsforholdFraSøknad = svangerskapspengerRepository.hentGrunnlag(referanse.getBehandlingId())
            .map(svpGrunnlagEntitet -> new TilretteleggingFilter(svpGrunnlagEntitet).getAktuelleTilretteleggingerUfiltrert())
            .orElse(Collections.emptyList());

        påkrevde.entrySet().forEach(entry -> {
            if (arbeidsforholdFraSøknad.stream().anyMatch(trlg -> trlg.getArbeidsgiver().map(arbeidsgiver -> arbeidsgiver.equals(entry.getKey())).orElse(false))) {
                filtrert.put(entry.getKey(), entry.getValue());
            }
        });
        return filtrert;
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                                  Map<Arbeidsgiver, Set<V>> påkrevde) {
        return påkrevde;
    }
}
