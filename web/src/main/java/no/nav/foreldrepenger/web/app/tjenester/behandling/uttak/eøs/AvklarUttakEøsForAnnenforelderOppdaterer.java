package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperiodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarUttakEøsForAnnenforelderDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarUttakEøsForAnnenforelderOppdaterer implements AksjonspunktOppdaterer<AvklarUttakEøsForAnnenforelderDto> {

    private final EøsUttakRepository eøsUttakRepository;
    private final HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public AvklarUttakEøsForAnnenforelderOppdaterer(EøsUttakRepository eøsUttakRepository, HistorikkinnslagRepository historikkinnslagRepository) {
        this.eøsUttakRepository = eøsUttakRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarUttakEøsForAnnenforelderDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        if (dto.getPerioder().isEmpty()) {
            historikkinnslagRepository.lagre(historikkinnslagIngenPerioder(dto, param));
        } else {
            historikkinnslagRepository.lagre(historikkinnslagRegistreringAvUttaksperioder(dto, param));
        }
        var eøsUttaksperioderEntitet = new EøsUttaksperioderEntitet.Builder()
            .leggTil(tilPerioderEntitet(dto.getPerioder())) //lagrer tomme hvis ingen perioder for å vise saksbehandlers avklaring
            .build();
        eøsUttakRepository.lagreEøsUttak(behandlingId, eøsUttaksperioderEntitet);
        return OppdateringResultat.utenTransisjon().build();
    }

    private List<EøsUttaksperiodeEntitet> tilPerioderEntitet(List<AvklarUttakEøsForAnnenforelderDto.EøsUttakPeriodeDto> perioder) {
        return perioder.stream()
            .map(AvklarUttakEøsForAnnenforelderOppdaterer::tilPeriodeEntitet)
            .toList();
    }

    private static EøsUttaksperiodeEntitet tilPeriodeEntitet(AvklarUttakEøsForAnnenforelderDto.EøsUttakPeriodeDto p) {
        return new EøsUttaksperiodeEntitet.Builder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(p.fom(), p.tom()))
            .medTrekkdager(new Trekkdager(p.trekkdager()))
            .medTrekkonto(p.trekkonto())
            .build();
    }

    private static Historikkinnslag historikkinnslagIngenPerioder(AvklarUttakEøsForAnnenforelderDto dto,
                                                                  AksjonspunktOppdaterParameter param) {
        return new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getBehandlingId())
            .medTittel(SkjermlenkeType.FAKTA_UTTAK_EØS)
            .addLinje("Avklart at annen forelder ikke har uttak i EØS")
            .addLinje(dto.getBegrunnelse())
            .build();
    }

    private static Historikkinnslag historikkinnslagRegistreringAvUttaksperioder(AvklarUttakEøsForAnnenforelderDto dto,
                                                                                 AksjonspunktOppdaterParameter param) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        linjer.add(new HistorikkinnslagLinjeBuilder().tekst("Registerer uttaksperioder for annen forelder i EØS"));
        for (var periode : dto.getPerioder()) {
            var trekkdager = new Trekkdager(periode.trekkdager());
            linjer.add(new HistorikkinnslagLinjeBuilder()
                .tekst(String.format("%s - %s: Forbrukt", periode.fom(), periode.tom()))
                .bold(String.format("%s dager av %s", trekkdager, periode.trekkonto().getNavn().toLowerCase())));
        }
        return new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getBehandlingId())
            .medTittel(SkjermlenkeType.FAKTA_UTTAK_EØS)
            .medLinjer(linjer)
            .addLinje(dto.getBegrunnelse())
            .build();
    }
}
