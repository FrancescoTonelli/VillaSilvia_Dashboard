import React from 'react';
import { 
    OnOffIcon, 
    BulbIcon, 
    SoundUpIcon, 
    SoundDownIcon,
    ColdIcon,
    HotIcon
 } from '../assets/Icons';

function ButtonOnOff({ onClick, classNameStatus, label = "ON" }) {
    return (
        <div className="button-description">
            <button onClick={onClick} className={`button-command ${classNameStatus}`}>
                <OnOffIcon fill="#ffffff" />
            </button>
            <p>{label}</p>
        </div>
    );
}

export function ButtonOn({ onClick }) {
    return <ButtonOnOff onClick={onClick} classNameStatus="button-on" label="ON" />;
}

export function ButtonOff({ onClick }) {
    return <ButtonOnOff onClick={onClick} classNameStatus="button-off" label="OFF" />;
}

export function ButtonToggle({ onClick }) {
    return <ButtonOnOff onClick={onClick} classNameStatus="button-toggle" label="ON/OFF" />;
}

function ButtonLight({ onClick, classNameStatus, label = "ALZA" }) {
    return (
        <div className="button-description">
            <button onClick={onClick} className={`button-command ${classNameStatus}`}>
                <BulbIcon />
            </button>
            <p>{label}</p>
        </div>
    );
}

export function ButtonLightDown({ onClick }) {
    return <ButtonLight onClick={onClick} classNameStatus="button-50" label="ABBASSA" />;
}

export function ButtonLightUp({ onClick }) {
    return <ButtonLight onClick={onClick} classNameStatus="button-100" label="ALZA" />;
}

export function ButtonSoundUp({ onClick }) {
    return (
        <div className="button-description">
            <button onClick={onClick} className={`button-command button-sound-up`}>
                <SoundUpIcon />
            </button>
            <p>ALZA</p>
        </div>
    );
}

export function ButtonSoundDown({ onClick }) {
    return (
        <div className="button-description">
            <button onClick={onClick} className={`button-command button-sound-down`}>
                <SoundDownIcon />
            </button>
            <p>ABBASSA</p>
        </div>
    );
}

export function ButtonCold({ onClick }) {
    return (
        <div className="button-description">
            <button onClick={onClick} className={`button-command button-cold`}>
                <ColdIcon fill="#ffffff" />
            </button>
            <p>FREDDA</p>
        </div>
    );
}

export function ButtonHot({ onClick }) {
    return (
        <div className="button-description">
            <button onClick={onClick} className={`button-command button-hot`}>
                <HotIcon fill="#ffffff" />
            </button>
            <p>CALDA</p>
        </div>
    );
}
