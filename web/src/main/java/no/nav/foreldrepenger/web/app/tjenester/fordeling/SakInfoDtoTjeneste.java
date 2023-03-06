package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kontrakter.fordel.SaksnummerDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class SakInfoDtoTjeneste {
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public SakInfoDtoTjeneste(BehandlingRepository behandlingRepository,
                              FamilieHendelseRepository familieHendelseRepository,
                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }
    public SakInfoDtoTjeneste() {
        //CDI
    }

    public SakInfoDto mapSakInfoDto(Fagsak fagsak) {
        var sisteYtelsesBehandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId()).orElse(null);
        if (sisteYtelsesBehandling != null) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(sisteYtelsesBehandling.getId());
        LocalDate førsteUttaksdato;
        try {
            førsteUttaksdato = stp.getFørsteUttaksdato();
        } catch (Exception e) {
            førsteUttaksdato = stp.getSkjæringstidspunktHvisUtledet().orElse(null);
        }
        var familiehendelseInfoDto = familieHendelseRepository.hentAggregatHvisEksisterer(sisteYtelsesBehandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(this::mapFamiliehendelseInfoDto);

            return mapTilSakInfoDto(fagsak, familiehendelseInfoDto.orElse(null), førsteUttaksdato);
        } else {
            return mapTilSakInfoDto(fagsak, null, null);
        }
    }

    private SakInfoDto.FamiliehendelseInfoDto mapFamiliehendelseInfoDto(FamilieHendelseEntitet familiehendelse) {
        return new SakInfoDto.FamiliehendelseInfoDto(familiehendelse.getSkjæringstidspunkt(), mapFamilieHendelseTypeDto(familiehendelse.getType()));
    }

    private SakInfoDto mapTilSakInfoDto(Fagsak fagsak, SakInfoDto.FamiliehendelseInfoDto familiehendelseInfoDto, LocalDate førsteUttaksdato) {
        return new SakInfoDto(new SaksnummerDto(fagsak.getSaksnummer().getVerdi()), mapFagsakYtelseTypeDto(fagsak.getYtelseType()), fagsak.getOpprettetTidspunkt().toLocalDate(),
            mapFagsakStatusDto(fagsak.getStatus()), familiehendelseInfoDto, førsteUttaksdato);
    }

    public SakInfoDto.FamilieHendelseTypeDto mapFamilieHendelseTypeDto(FamilieHendelseType familieHendelseType) {
        return switch (familieHendelseType) {
            case FØDSEL -> SakInfoDto.FamilieHendelseTypeDto.FØDSEL;
            case TERMIN -> SakInfoDto.FamilieHendelseTypeDto.TERMIN;
            case ADOPSJON -> SakInfoDto.FamilieHendelseTypeDto.ADOPSJON;
            case OMSORG -> SakInfoDto.FamilieHendelseTypeDto.OMSORG;
            case UDEFINERT -> null;
        };
    }

    public SakInfoDto.FagsakYtelseTypeDto mapFagsakYtelseTypeDto(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> SakInfoDto.FagsakYtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> SakInfoDto.FagsakYtelseTypeDto.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD -> SakInfoDto.FagsakYtelseTypeDto.ENGANGSTØNAD;
            case UDEFINERT -> throw new IllegalStateException("Mangler fagsakYtelseType");
        };
    }

    public SakInfoDto.FagsakStatusDto mapFagsakStatusDto(FagsakStatus fagsakStatus) {
        return switch (fagsakStatus) {
            case OPPRETTET, UNDER_BEHANDLING -> SakInfoDto.FagsakStatusDto.UNDER_BEHANDLING;
            case LØPENDE -> SakInfoDto.FagsakStatusDto.LØPENDE;
            case AVSLUTTET -> SakInfoDto.FagsakStatusDto.AVSLUTTET;
        };
    }
}
