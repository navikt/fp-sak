package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InaktiveArbeidsforholdUtleder;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingFilterYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

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
    public Set<Arbeidsgiver> søknadsFilter(BehandlingReferanse referanse, Set<Arbeidsgiver> påkrevde) {
        var arbeidsgivereFraSøknad = getArbeidsgivereSøktTilretteleggingI(referanse);
        return påkrevde.stream()
            .filter(arbeidsgivereFraSøknad::contains)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<Arbeidsgiver> aktiveArbeidsforholdFilter(BehandlingReferanse referanse,
                                                        Skjæringstidspunkt stp,
                                                        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                        Set<Arbeidsgiver> påkrevde) {
        var aktive = new HashSet<>(InaktiveArbeidsforholdUtleder.finnKunAktive(påkrevde, inntektArbeidYtelseGrunnlag, referanse, stp));

        // Legger inn alle arbeidsgivere det er søkt tilrettelegging hos
        var arbeidsgivereFraSøknad = getArbeidsgivereSøktTilretteleggingI(referanse);
        påkrevde.stream()
            .filter(arbeidsgivereFraSøknad::contains)
            .forEach(aktive::add);
        return aktive;
    }

    private Set<Arbeidsgiver> getArbeidsgivereSøktTilretteleggingI(BehandlingReferanse referanse) {
        return getTilretteleggingerFraSøknad(referanse).stream()
            .flatMap(trlg -> trlg.getArbeidsgiver().stream())
            .collect(Collectors.toSet());
    }

    private List<SvpTilretteleggingEntitet> getTilretteleggingerFraSøknad(BehandlingReferanse referanse) {
        return svangerskapspengerRepository.hentGrunnlag(referanse.behandlingId())
            .map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElse(Collections.emptyList());
    }
}
