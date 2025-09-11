package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.Collection;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOpprettingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.AnnenPartBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.FagsakBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkinnslagDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.dto.KontrollresultatDto;

public record FagsakFullDto(@NotNull String saksnummer,
                            @NotNull FagsakYtelseType fagsakYtelseType,
                            @NotNull RelasjonsRolleType relasjonsRolleType,
                            @NotNull FagsakStatus status,
                            @NotNull String aktørId,
                            @NotNull boolean sakSkalTilInfotrygd,
                            @NotNull Integer dekningsgrad,
                            @NotNull PersonDto bruker,
                            @NotNull boolean brukerManglerAdresse,
                            PersonDto annenPart,
                            AnnenPartBehandlingDto annenpartBehandling,
                            SakHendelseDto familiehendelse,
                            @NotNull List<FagsakMarkeringDto> fagsakMarkeringer,
                            @NotNull List<BehandlingOpprettingDto> behandlingTypeKanOpprettes,
                            @NotNull List<FagsakBehandlingDto> behandlinger,
                            @NotNull List<HistorikkinnslagDto> historikkinnslag,
                            @NotNull List<FagsakNotatDto> notater,
                            @NotNull KontrollresultatDto kontrollResultat,
                            @NotNull boolean harVergeIÅpenBehandling) {

    public FagsakFullDto(Fagsak fagsak, Integer dekningsgrad, PersonDto bruker,
                         boolean brukerManglerAdresse,
                         PersonDto annenPart,
                         AnnenPartBehandlingDto annenpartBehandling,
                         SakHendelseDto familiehendelse,
                         Collection<FagsakMarkering> fagsakMarkeringer,
                         List<BehandlingOpprettingDto> behandlingTypeKanOpprettes,
                         List<FagsakBehandlingDto> behandlinger,
                         List<HistorikkinnslagDto> historikkinnslag,
                         List<FagsakNotatDto> notater,
                         KontrollresultatDto kontrollResultat,
                         boolean harVergeIÅpenBehandling) {
        this(fagsak.getSaksnummer().getVerdi(), fagsak.getYtelseType(), fagsak.getRelasjonsRolleType(), fagsak.getStatus(),
            fagsak.getAktørId().getId(), fagsak.erStengt(), dekningsgrad, bruker, brukerManglerAdresse, annenPart, annenpartBehandling,
            familiehendelse, fagsakMarkeringer.stream().map(fm -> new FagsakMarkeringDto(fm, fm.getKortNavn())).toList(),
            behandlingTypeKanOpprettes, behandlinger, historikkinnslag, notater, kontrollResultat, harVergeIÅpenBehandling);
    }
}
