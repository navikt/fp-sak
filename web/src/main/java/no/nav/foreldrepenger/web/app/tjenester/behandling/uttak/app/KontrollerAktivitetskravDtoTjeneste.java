package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.fakta.aktkrav.KontrollerAktivitetskravAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravPeriodeDto;

@ApplicationScoped
public class KontrollerAktivitetskravDtoTjeneste {

    private BehandlingRepository behandlingRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    @Inject
    public KontrollerAktivitetskravDtoTjeneste(BehandlingRepository behandlingRepository,
                                               YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                               UttakInputTjeneste uttakInputTjeneste,
                                               ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
    }

    KontrollerAktivitetskravDtoTjeneste() {
        //CDI
    }

    public List<KontrollerAktivitetskravPeriodeDto> lagDtos(@NotNull @Valid UuidDto behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId.getBehandlingUuid());
        if (!behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER)) {
            throw new IllegalArgumentException("Støtter bare foreldrepenger");
        }
        var ytelseFordelingAggregatOpt = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        if (ytelseFordelingAggregatOpt.isEmpty()) {
            return List.of();
        }
        var ytelseFordelingAggregat = ytelseFordelingAggregatOpt.get();
        /* Mtp eldre saker i prod der avklaring av aktivitetskrav ble gjort i uttaksbildet.
        Forvirrende hvis det vises manglende avklaringer i etterkant */
        if (ytelseFordelingAggregat.getGjeldendeAktivitetskravPerioder().isEmpty()
            && (behandling.erAvsluttet() || !BehandlingStegType.KONTROLLER_AKTIVITETSKRAV.equals(behandling.getAktivtBehandlingSteg()))) {
            return List.of();
        }
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var uttakInput = uttakInputTjeneste.lagInput(behandlingReferanse.behandlingId());
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var familieHendelse = ytelsespesifiktGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        var annenForelderHarRett = harAnnenForelderRett(ytelseFordelingAggregat, ytelsespesifiktGrunnlag);

        var result = new ArrayList<KontrollerAktivitetskravPeriodeDto>();
        for (var søknadsperiode : ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder()) {
            var avklaringsresultat = KontrollerAktivitetskravAksjonspunktUtleder.skalKontrollereAktivitetskrav(
                behandlingReferanse, søknadsperiode, ytelseFordelingAggregat, familieHendelse, annenForelderHarRett);
            if (avklaringsresultat.isKravTilAktivitet()) {
                if (avklaringsresultat.isAvklart()) {
                    var avklartePerioder = avklaringsresultat.getAvklartePerioder();
                    for (var avklartPeriode : avklartePerioder) {
                        var dto = new KontrollerAktivitetskravPeriodeDto();
                        dto.setAvklaring(avklartPeriode.getAvklaring());
                        dto.setBegrunnelse(avklartPeriode.getBegrunnelse());
                        dto.setFom(max(avklartPeriode.getTidsperiode().getFomDato(), søknadsperiode.getFom()));
                        dto.setTom(min(avklartPeriode.getTidsperiode().getTomDato(), søknadsperiode.getTom()));
                        dto.setEndret(erEndretDenneBehandling(avklartPeriode, ytelseFordelingAggregat));
                        dto.setMorsAktivitet(søknadsperiode.getMorsAktivitet());
                        result.add(dto);
                    }
                } else {
                    var dto = new KontrollerAktivitetskravPeriodeDto();
                    dto.setFom(søknadsperiode.getFom());
                    dto.setTom(søknadsperiode.getTom());
                    dto.setMorsAktivitet(søknadsperiode.getMorsAktivitet());
                    result.add(dto);
                }
            }
        }
        return result;
    }

    private boolean erEndretDenneBehandling(AktivitetskravPeriodeEntitet avklartPeriode,
                                            YtelseFordelingAggregat ytelseFordelingAggregat) {
        var opprinneligeAktivitetskravPerioder = ytelseFordelingAggregat.getOpprinneligeAktivitetskravPerioder();
        if (opprinneligeAktivitetskravPerioder.isEmpty()) {
            return true;
        }
        return opprinneligeAktivitetskravPerioder.get()
            .getPerioder()
            .stream()
            .noneMatch(p -> equals(avklartPeriode, p));
    }

    private boolean equals(AktivitetskravPeriodeEntitet avklartPeriode, AktivitetskravPeriodeEntitet p) {
        return avklartPeriode.getTidsperiode().equals(p.getTidsperiode()) && avklartPeriode.getAvklaring()
            .equals(p.getAvklaring()) && avklartPeriode.getBegrunnelse().equals(p.getBegrunnelse());
    }

    private boolean harAnnenForelderRett(YtelseFordelingAggregat ytelseFordelingAggregat,
                                         ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var annenpartUttak = ytelsespesifiktGrunnlag.getAnnenpart()
            .map(Annenpart::gjeldendeVedtakBehandlingId)
            .flatMap(foreldrepengerUttakTjeneste::hentUttakHvisEksisterer);
        return UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat, annenpartUttak);
    }


    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
