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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InaktiveArbeidsforholdUtleder;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingFilterYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
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
        List<SvpTilretteleggingEntitet> arbeidsforholdFraSøknad = getArbeidsforholdSøktTilretteleggingI(referanse);
        påkrevde.forEach((key, value) -> {
            if (erSøktTilretteleggingI(arbeidsforholdFraSøknad, key)) {
                filtrert.put(key, value);
            }
        });
        return filtrert;
    }

    private boolean erSøktTilretteleggingI(List<SvpTilretteleggingEntitet> arbeidsforholdFraSøknad, Arbeidsgiver key) {
        return arbeidsforholdFraSøknad.stream()
                .anyMatch(trlg -> trlg.getArbeidsgiver().map(arbeidsgiver -> arbeidsgiver.equals(key)).orElse(false));
    }

    private List<SvpTilretteleggingEntitet> getArbeidsforholdSøktTilretteleggingI(BehandlingReferanse referanse) {
        return svangerskapspengerRepository.hentGrunnlag(referanse.behandlingId())
            .map(svpGrunnlagEntitet -> new TilretteleggingFilter(svpGrunnlagEntitet).getAktuelleTilretteleggingerUfiltrert())
            .orElse(Collections.emptyList());
    }

    @Override
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse,
            Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
            Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevde) {
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> kunAktive = InaktiveArbeidsforholdUtleder.finnKunAktive(påkrevde, inntektArbeidYtelseGrunnlag, referanse);

        // Legger inn alle arbeidsforhold det er søkt tilrettelegging i
        List<SvpTilretteleggingEntitet> arbeidsforholdFraSøknad = getArbeidsforholdSøktTilretteleggingI(referanse);
        påkrevde.forEach((key, value) -> {
            if (erSøktTilretteleggingI(arbeidsforholdFraSøknad, key) && !kunAktive.containsKey(key)) {
                kunAktive.put(key, value);
            }
        });
        return kunAktive;
    }
}
