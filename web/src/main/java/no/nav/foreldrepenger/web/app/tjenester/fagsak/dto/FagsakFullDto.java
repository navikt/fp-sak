package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.Collection;
import java.util.List;

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

public record FagsakFullDto(String saksnummer,
                            FagsakYtelseType fagsakYtelseType,
                            RelasjonsRolleType relasjonsRolleType,
                            FagsakStatus status,
                            String aktørId,
                            boolean sakSkalTilInfotrygd,
                            Integer dekningsgrad,
                            PersonDto bruker,
                            boolean brukerManglerAdresse,
                            PersonDto annenPart,
                            AnnenPartBehandlingDto annenpartBehandling,
                            SakHendelseDto familiehendelse,
                            List<FagsakMarkeringDto> fagsakMarkeringer,
                            List<BehandlingOpprettingDto> behandlingTypeKanOpprettes,
                            List<FagsakBehandlingDto> behandlinger,
                            List<HistorikkinnslagDto> historikkinnslag,
                            List<FagsakNotatDto> notater,
                            KontrollresultatDto kontrollResultat) {

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
                         KontrollresultatDto kontrollResultat) {
        this(fagsak.getSaksnummer().getVerdi(), fagsak.getYtelseType(), fagsak.getRelasjonsRolleType(), fagsak.getStatus(),
            fagsak.getAktørId().getId(), fagsak.erStengt(), dekningsgrad, bruker, brukerManglerAdresse, annenPart, annenpartBehandling,
            familiehendelse, fagsakMarkeringer.stream().map(fm -> new FagsakMarkeringDto(fm, fm.getKortNavn())).toList(),
            behandlingTypeKanOpprettes, behandlinger, historikkinnslag, notater, kontrollResultat);
    }
}
