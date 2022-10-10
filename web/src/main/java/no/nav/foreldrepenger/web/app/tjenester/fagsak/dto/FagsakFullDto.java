package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOpprettingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.AnnenPartBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDto;

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
                            List<BehandlingOpprettingDto> behandlingTypeKanOpprettes,
                            List<BehandlingDto> behandlinger) {

    public FagsakFullDto(Fagsak fagsak, Integer dekningsgrad, PersonDto bruker,
                         boolean brukerManglerAdresse,
                         PersonDto annenPart,
                         AnnenPartBehandlingDto annenpartBehandling,
                         SakHendelseDto familiehendelse,
                         List<BehandlingOpprettingDto> behandlingTypeKanOpprettes,
                         List<BehandlingDto> behandlinger) {
        this(fagsak.getSaksnummer().getVerdi(), fagsak.getYtelseType(), fagsak.getRelasjonsRolleType(), fagsak.getStatus(), fagsak.getAktørId().getId(),
            fagsak.erStengt(), dekningsgrad, bruker, brukerManglerAdresse, annenPart, annenpartBehandling,
            familiehendelse, behandlingTypeKanOpprettes, behandlinger);
    }
}
