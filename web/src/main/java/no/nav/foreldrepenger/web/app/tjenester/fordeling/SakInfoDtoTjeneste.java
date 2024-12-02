package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kontrakter.fordel.SakInfoV2Dto;
import no.nav.foreldrepenger.kontrakter.fordel.SaksnummerDto;
import no.nav.foreldrepenger.kontrakter.fordel.YtelseTypeDto;
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

    public SakInfoV2Dto mapSakInfoV2Dto(Fagsak fagsak) {
        var sisteYtelsesBehandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId()).orElse(null);
        if (sisteYtelsesBehandling != null) {
            LocalDate førsteUttaksdato = finnFørsteUttaksdato(sisteYtelsesBehandling);
            var familiehendelseInfoDto = familieHendelseRepository.hentAggregatHvisEksisterer(sisteYtelsesBehandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(this::mapFamiliehendelseInfoV2Dto);

            return mapTilSakInfoV2Dto(fagsak, familiehendelseInfoDto.orElse(null), førsteUttaksdato);
        } else {
            return mapTilSakInfoV2Dto(fagsak, null, null);
        }
    }

    private LocalDate finnFørsteUttaksdato(Behandling behandling) {
        try {
            var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
            try {
                return stp.getFørsteUttaksdato();
            } catch (Exception e) {
                return stp.getSkjæringstidspunktHvisUtledet().orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private SakInfoV2Dto mapTilSakInfoV2Dto(Fagsak fagsak, SakInfoV2Dto.FamiliehendelseInfoDto familiehendelseInfoDto, LocalDate førsteUttaksdato) {
        return new SakInfoV2Dto(new SaksnummerDto(fagsak.getSaksnummer().getVerdi()),
            mapYtelseTypeV2Dto(fagsak.getYtelseType()),
            mapFagsakStatusV2Dto(fagsak.getStatus()),
            familiehendelseInfoDto,
            fagsak.getOpprettetTidspunkt().toLocalDate(),
            førsteUttaksdato);
    }

    public YtelseTypeDto mapYtelseTypeV2Dto(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD -> YtelseTypeDto.ENGANGSTØNAD;
            case UDEFINERT -> throw new IllegalStateException("Mangler fagsakYtelseType");
        };
    }

    public SakInfoV2Dto.FagsakStatusDto mapFagsakStatusV2Dto(FagsakStatus fagsakStatus) {
        return switch (fagsakStatus) {
            case OPPRETTET, UNDER_BEHANDLING -> SakInfoV2Dto.FagsakStatusDto.UNDER_BEHANDLING;
            case LØPENDE -> SakInfoV2Dto.FagsakStatusDto.LØPENDE;
            case AVSLUTTET -> SakInfoV2Dto.FagsakStatusDto.AVSLUTTET;
        };
    }

    private SakInfoV2Dto.FamiliehendelseInfoDto mapFamiliehendelseInfoV2Dto(FamilieHendelseEntitet familiehendelse) {
        return new SakInfoV2Dto.FamiliehendelseInfoDto(familiehendelse.getSkjæringstidspunkt(), mapFamilieHendelseTypeV2Dto(familiehendelse.getType()));
    }

    public SakInfoV2Dto.FamilieHendelseTypeDto mapFamilieHendelseTypeV2Dto(FamilieHendelseType familieHendelseType) {
        return switch (familieHendelseType) {
            case FØDSEL -> SakInfoV2Dto.FamilieHendelseTypeDto.FØDSEL;
            case TERMIN -> SakInfoV2Dto.FamilieHendelseTypeDto.TERMIN;
            case ADOPSJON -> SakInfoV2Dto.FamilieHendelseTypeDto.ADOPSJON;
            case OMSORG -> SakInfoV2Dto.FamilieHendelseTypeDto.OMSORG;
            case UDEFINERT -> null; // bør ikke være mulig - det ligger ca 335 inaktive hendelser fra 2018 (antageligvis en feil)
        };
    }
}
