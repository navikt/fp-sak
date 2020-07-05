package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import static no.nav.foreldrepenger.skjæringstidspunkt.svp.BeregnTilrettleggingsdato.beregn;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste, SkjæringstidspunktRegisterinnhentingTjeneste {

    private SvangerskapspengerRepository svangerskapspengerRepository;
    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;

    SkjæringstidspunktTjenesteImpl() {
        //CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(SvangerskapspengerRepository svangerskapspengerRepository,
                                          OpptjeningRepository opptjeningRepository,
                                          BehandlingRepository behandlingRepository) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Skjæringstidspunkt.Builder builder = Skjæringstidspunkt.builder();
        LocalDate skjæringstidspunkt = utledSkjæringstidspunkt(behandlingId);
        builder.medFørsteUttaksdato(skjæringstidspunkt);
        builder.medUtledetSkjæringstidspunkt(skjæringstidspunkt);
        builder.medSkjæringstidspunktOpptjening(skjæringstidspunkt);
        return builder.build();
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        return utledSkjæringstidspunktRegisterinnhenting(behandlingId);
    }

    private LocalDate utledSkjæringstidspunkt(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<Opptjening> opptjeningOpt = opptjeningRepository.finnOpptjening(behandlingId);

        // Ved revurderinger beregner vi alltid skjæringstidspunkt på nytt
        if (opptjeningOpt.isPresent() && !behandling.erRevurdering()) {
            return opptjeningOpt.get().getTom().plusDays(1);
        }

        Optional<SvpGrunnlagEntitet> svpGrunnlagOpt = svangerskapspengerRepository.hentGrunnlag(behandlingId);
        //TODO(OJR) en svakhet?
        // Dagens dato blir gitt når grunnlag ikke finnes for at DTOer skal fungere.
        return svpGrunnlagOpt.map(this::utledBasertPåGrunnlag).orElse(LocalDate.now());
    }

    LocalDate utledBasertPåGrunnlag(SvpGrunnlagEntitet grunnlag) {
        var tidligsteTilretteleggingsDatoOpt = new TilretteleggingFilter(grunnlag)
            .getAktuelleTilretteleggingerFiltrert().stream()
            .map(aktuelle -> beregn(aktuelle.getBehovForTilretteleggingFom(),
                aktuelle.getTilretteleggingFOMListe().stream()
                    .filter(tl -> tl.getType().equals(TilretteleggingType.HEL_TILRETTELEGGING))
                    .map(TilretteleggingFOM::getFomDato)
                    .min(LocalDate::compareTo),
                aktuelle.getTilretteleggingFOMListe().stream()
                    .filter(tl -> tl.getType().equals(TilretteleggingType.DELVIS_TILRETTELEGGING))
                    .map(TilretteleggingFOM::getFomDato)
                    .min(LocalDate::compareTo),
                aktuelle.getTilretteleggingFOMListe().stream()
                    .filter(tl -> tl.getType().equals(TilretteleggingType.INGEN_TILRETTELEGGING))
                    .map(TilretteleggingFOM::getFomDato)
                    .min(LocalDate::compareTo)))
            .min(Comparator.naturalOrder());
        return tidligsteTilretteleggingsDatoOpt.orElseThrow(() -> new IllegalStateException("Klarte ikke finne skjæringstidspunkt for SVP"));
    }

    private LocalDate utledSkjæringstidspunktRegisterinnhenting(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<SvpGrunnlagEntitet> svpGrunnlagOpt = svangerskapspengerRepository.hentGrunnlag(behandlingId);
        if (svpGrunnlagOpt.isPresent()) {
            SvpGrunnlagEntitet grunnlag = svpGrunnlagOpt.get();
            // Bruk "jordmordato" som stabil referanse
            var tidligsteTilretteleggingsDatoOpt = new TilretteleggingFilter(grunnlag)
                .getAktuelleTilretteleggingerFiltrert().stream()
                .map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom)
                .min(Comparator.naturalOrder());
            if (tidligsteTilretteleggingsDatoOpt.isPresent()) {
                return tidligsteTilretteleggingsDatoOpt.get();
            }
        }
        Optional<Opptjening> opptjeningOpt = opptjeningRepository.finnOpptjening(behandlingId);
        if (!behandling.erRevurdering() && opptjeningOpt.map(Opptjening::erOpptjeningPeriodeVilkårOppfylt).orElse(Boolean.FALSE)) {
            return opptjeningOpt.get().getTom().plusDays(1);
        }
        //TODO(OJR) en svakhet?
        // Har ikke grunnlag for å avgjøre skjæringstidspunkt enda så gir midlertidig dagens dato. for at DTOer skal fungere.
        return LocalDate.now();
    }
}
