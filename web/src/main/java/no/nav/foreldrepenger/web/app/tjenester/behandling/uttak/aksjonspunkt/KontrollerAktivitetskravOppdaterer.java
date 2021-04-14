package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet.fraOgMedTilOgMed;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.fakta.aktkrav.KontrollerAktivitetskravAksjonspunktUtleder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravPeriodeDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollerAktivitetskravDto.class, adapter = AksjonspunktOppdaterer.class)
public class KontrollerAktivitetskravOppdaterer implements AksjonspunktOppdaterer<KontrollerAktivitetskravDto> {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private KontrollerAktivitetskravHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private KontrollerAktivitetskravAksjonspunktUtleder utleder;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public KontrollerAktivitetskravOppdaterer(YtelsesFordelingRepository ytelsesFordelingRepository,
                                              KontrollerAktivitetskravHistorikkinnslagTjeneste historikkinnslagTjeneste,
                                              KontrollerAktivitetskravAksjonspunktUtleder utleder,
                                              UttakInputTjeneste uttakInputTjeneste) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.utleder = utleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    KontrollerAktivitetskravOppdaterer() {
        //CDI
    }

    @Override
    public OppdateringResultat oppdater(KontrollerAktivitetskravDto dto, AksjonspunktOppdaterParameter param) {
        valider(dto.getAvklartePerioder());
        var eksisterendePerioder = ytelsesFordelingRepository.hentAggregat(param.getBehandlingId())
            .getGjeldendeAktivitetskravPerioder()
            .stream()
            .flatMap(ap -> ap.getPerioder().stream())
            .collect(Collectors.toList());
        opprettHistorikkinnslag(param.getBehandlingId(), dto, eksisterendePerioder);
        lagre(param.getBehandlingId(), dto);
        var resultat = OppdateringResultat.utenTransisjon();
        if (!harLøstAksjonspunktet(param)) {
            resultat.medBeholdAksjonspunktÅpent();
        }
        return resultat.build();
    }

    private boolean harLøstAksjonspunktet(AksjonspunktOppdaterParameter param) {
        var uttakInput = uttakInputTjeneste.lagInput(param.getBehandlingId());
        return utleder.utledFor(uttakInput).isEmpty();
    }

    private void opprettHistorikkinnslag(Long behandlingId,
                                         KontrollerAktivitetskravDto dto,
                                         List<AktivitetskravPeriodeEntitet> eksisterendePerioder) {
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandlingId, dto, eksisterendePerioder);
    }

    private void lagre(Long behandlingId, KontrollerAktivitetskravDto dto) {
        var entiteter = map(dto.getAvklartePerioder());
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var avklartPeriodeFørEndringsdato = avklartePerioderFørEndringsdato(ytelseFordelingAggregat);
        avklartPeriodeFørEndringsdato.forEach(entiteter::leggTil);
        var oppdatertYtelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medSaksbehandledeAktivitetskravPerioder(entiteter)
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, oppdatertYtelseFordelingAggregat);
    }

    private List<AktivitetskravPeriodeEntitet> avklartePerioderFørEndringsdato(YtelseFordelingAggregat ytelseFordelingAggregat) {
        var aktivitetskravPerioder = ytelseFordelingAggregat.getGjeldendeAktivitetskravPerioder();
        if (aktivitetskravPerioder.isEmpty()) {
            return List.of();
        }
        var endringsdato = ytelseFordelingAggregat.getGjeldendeEndringsdato();
        var avklartPeriodeFørEndringsdato = aktivitetskravPerioder.get()
            .getPerioder()
            .stream()
            .filter(p -> p.getTidsperiode().getFomDato().isBefore(endringsdato))
            .map(p -> kopier(p))
            .sorted(Comparator.comparing(o -> o.getTidsperiode().getFomDato()))
            .collect(Collectors.toList());
        //Knekke siste periode ved overlapp med endringsdato
        if (avklartPeriodeFørEndringsdato.size() > 0) {
            var sistePeriode = avklartPeriodeFørEndringsdato.get(avklartPeriodeFørEndringsdato.size() - 1);
            if (sistePeriode.getTidsperiode().inkluderer(endringsdato)) {
                var kopier = new AktivitetskravPeriodeEntitet(sistePeriode.getTidsperiode().getFomDato(),
                    endringsdato.minusDays(1), sistePeriode.getAvklaring(), sistePeriode.getBegrunnelse());
                avklartPeriodeFørEndringsdato.set(avklartPeriodeFørEndringsdato.size() - 1, kopier);
            }
        }
        return avklartPeriodeFørEndringsdato;
    }

    private AktivitetskravPeriodeEntitet kopier(AktivitetskravPeriodeEntitet p) {
        return new AktivitetskravPeriodeEntitet(p);
    }

    private AktivitetskravPerioderEntitet map(List<KontrollerAktivitetskravPeriodeDto> perioder) {
        var entitet = new AktivitetskravPerioderEntitet();
        for (var dtoPeriode : perioder) {
            entitet.leggTil(map(dtoPeriode));
        }
        return entitet;
    }

    private AktivitetskravPeriodeEntitet map(KontrollerAktivitetskravPeriodeDto dtoPeriode) {
        return new AktivitetskravPeriodeEntitet(dtoPeriode.getFom(), dtoPeriode.getTom(), dtoPeriode.getAvklaring(),
            dtoPeriode.getBegrunnelse());
    }

    private void valider(List<KontrollerAktivitetskravPeriodeDto> perioder) {
        validerOverlapp(perioder);
    }

    private void validerOverlapp(List<KontrollerAktivitetskravPeriodeDto> perioder) {
        for (var i = 0; i < perioder.size() - 1; i++) {
            for (var j = i + 1; j < perioder.size(); j++) {
                var tidsperiode1 = fraOgMedTilOgMed(perioder.get(i).getFom(), perioder.get(i).getTom());
                var tidsperiode2 = fraOgMedTilOgMed(perioder.get(j).getFom(), perioder.get(j).getTom());
                if (tidsperiode1.overlapper(tidsperiode2)) {
                    throw new IllegalArgumentException("Overlapp i perioder: " + perioder);
                }
            }
        }
    }
}
