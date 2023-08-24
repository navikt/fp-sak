package no.nav.foreldrepenger.behandling.revurdering;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.*;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Lag historikk innslag ved revurdering.
 */
public class RevurderingHistorikk {
    private HistorikkRepository historikkRepository;

    public RevurderingHistorikk(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslagOmRevurdering(Behandling behandling, BehandlingÅrsakType revurderingsÅrsak, boolean manueltOpprettet) {
        if (BehandlingÅrsakType.alleTekniskeÅrsaker().contains(revurderingsÅrsak)) {
            return;
        }

        var historikkAktør = manueltOpprettet ? HistorikkAktør.SAKSBEHANDLER : HistorikkAktør.VEDTAKSLØSNINGEN;

        var revurderingsInnslag = new Historikkinnslag();
        revurderingsInnslag.setBehandling(behandling);
        revurderingsInnslag.setType(HistorikkinnslagType.REVURD_OPPR);
        revurderingsInnslag.setAktør(historikkAktør);
        var historiebygger = new HistorikkInnslagTekstBuilder()
                .medHendelse(HistorikkinnslagType.REVURD_OPPR)
                .medBegrunnelse(revurderingsÅrsak);
        historiebygger.build(revurderingsInnslag);

        historikkRepository.lagre(revurderingsInnslag);
    }

    public void opprettHistorikkinnslagForFødsler(Behandling behandling, List<FødtBarnInfo> barnFødtIPeriode) {
        var fødselInnslag = new Historikkinnslag();
        fødselInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        fødselInnslag.setType(HistorikkinnslagType.NY_INFO_FRA_TPS);
        fødselInnslag.setBehandling(behandling);

        var dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String fødselsdatoVerdi;
        if (barnFødtIPeriode.size() > 1) {
            SortedSet<LocalDate> fødselsdatoer = new TreeSet<>(
                    barnFødtIPeriode.stream().map(FødtBarnInfo::fødselsdato).collect(Collectors.toSet()));
            fødselsdatoVerdi = fødselsdatoer.stream().map(dateFormat::format).collect(Collectors.joining(", "));
        } else {
            fødselsdatoVerdi = dateFormat.format(barnFødtIPeriode.get(0).fødselsdato());
        }
        var historieBuilder = new HistorikkInnslagTekstBuilder()
                .medHendelse(HistorikkinnslagType.NY_INFO_FRA_TPS)
                .medOpplysning(HistorikkOpplysningType.FODSELSDATO, fødselsdatoVerdi)
                .medOpplysning(HistorikkOpplysningType.TPS_ANTALL_BARN, barnFødtIPeriode.size());
        historieBuilder.build(fødselInnslag);
        historikkRepository.lagre(fødselInnslag);

    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Long behandlingId,
            Long fagsakId,
            HistorikkinnslagType historikkinnslagType,
            LocalDateTime fristTid,
            Venteårsak venteårsak) {
        var builder = new HistorikkInnslagTekstBuilder();
        if (fristTid != null) {
            builder.medHendelse(historikkinnslagType, fristTid.toLocalDate());
        } else {
            builder.medHendelse(historikkinnslagType);
        }
        if (venteårsak != null) {
            builder.medÅrsak(venteårsak);
        }
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandlingId);
        historikkinnslag.setFagsakId(fagsakId);
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

}
