package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import static no.nav.foreldrepenger.skjæringstidspunkt.svp.BeregnTilrettleggingsdato.beregn;

import java.time.LocalDate;
import java.util.Comparator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
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
import no.nav.fpsak.tidsserie.LocalDateInterval;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste, SkjæringstidspunktRegisterinnhentingTjeneste {

    private static final int MAX_SVANGERSKAP_UKER = 42;

    private SvangerskapspengerRepository svangerskapspengerRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;

    SkjæringstidspunktTjenesteImpl() {
        //CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(SvangerskapspengerRepository svangerskapspengerRepository,
                                          FamilieHendelseRepository familieHendelseRepository,
                                          OpptjeningRepository opptjeningRepository,
                                          BehandlingRepository behandlingRepository) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        var skjæringstidspunkt = utledSkjæringstidspunkt(behandlingId);
        return Skjæringstidspunkt.builder()
            .medFørsteUttaksdato(skjæringstidspunkt)
            .medGrunnbeløpdato(skjæringstidspunkt)
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt)
            .medUtledetMedlemsintervall(utledYtelseintervall(behandlingId, skjæringstidspunkt))
            .build();
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        return utledSkjæringstidspunktRegisterinnhenting(behandlingId);
    }

    private LocalDate utledSkjæringstidspunkt(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var opptjeningOpt = opptjeningRepository.finnOpptjening(behandlingId);

        // Ved revurderinger beregner vi alltid skjæringstidspunkt på nytt
        if (opptjeningOpt.isPresent() && !behandling.erRevurdering()) {
            return opptjeningOpt.get().getTom().plusDays(1);
        }

        var svpGrunnlagOpt = svangerskapspengerRepository.hentGrunnlag(behandlingId);
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
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var svpGrunnlagOpt = svangerskapspengerRepository.hentGrunnlag(behandlingId);
        if (svpGrunnlagOpt.isPresent()) {
            var grunnlag = svpGrunnlagOpt.get();
            // Bruk "jordmordato" som stabil referanse
            var tidligsteTilretteleggingsDatoOpt = new TilretteleggingFilter(grunnlag)
                .getAktuelleTilretteleggingerFiltrert().stream()
                .map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom)
                .min(Comparator.naturalOrder());
            if (tidligsteTilretteleggingsDatoOpt.isPresent()) {
                return tidligsteTilretteleggingsDatoOpt.get();
            }
        }
        var opptjeningOpt = opptjeningRepository.finnOpptjening(behandlingId);
        if (!behandling.erRevurdering() && opptjeningOpt.map(Opptjening::erOpptjeningPeriodeVilkårOppfylt).orElse(Boolean.FALSE)) {
            return opptjeningOpt.get().getTom().plusDays(1);
        }
        //TODO(OJR) en svakhet?
        // Har ikke grunnlag for å avgjøre skjæringstidspunkt enda så gir midlertidig dagens dato. for at DTOer skal fungere.
        return LocalDate.now();
    }

    private LocalDateInterval utledYtelseintervall(Long behandlingId, LocalDate skjæringstidspunkt) {
        try {
            var antattTom = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
                .orElse(skjæringstidspunkt.plusWeeks(MAX_SVANGERSKAP_UKER));
            return new LocalDateInterval(skjæringstidspunkt, antattTom);
        } catch (Exception e) {
            return new LocalDateInterval(skjæringstidspunkt, skjæringstidspunkt.plusWeeks(MAX_SVANGERSKAP_UKER));
        }
    }
}
